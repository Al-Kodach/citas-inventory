package com.citacomsys.backup.util

import com.citacomsys.backup.api.RestoreResult
import com.citacomsys.backup.api.RestoreScope
import com.citacomsys.backup.api.fileUtil.BackupArchiveService
import com.citacomsys.backup.api.fileUtil.BackupCryptoService
import com.citacomsys.backup.api.model.BackupManifest
import com.citacomsys.core.database.backup.DatabaseBackupDriver
import com.citacomsys.core.database.backup.RestoreScope as CoreRestoreScope
import com.citacomsys.core.domain.AppInfoProvider
import com.citacomsys.core.domain.file.PlatformFileSystem
import kotlinx.serialization.json.Json
import okio.Path

internal class RestoreCoordinator(
    private val dbDriver: DatabaseBackupDriver,
    private val archive: BackupArchiveService,
    private val crypto: BackupCryptoService,
    private val appInfo: AppInfoProvider,
    private val fileSystem: PlatformFileSystem
) {
    suspend fun restore(
        backupPathString: String,
        password: String,
        scope: RestoreScope = RestoreScope.BOTH
    ): RestoreResult {
        val backupPath = fileSystem.getInternalDir() / backupPathString
        var tempDir: Path? = null

        val coreScope = when (scope) {
            RestoreScope.INVENTORY -> CoreRestoreScope.INVENTORY
            RestoreScope.CUSTOMER -> CoreRestoreScope.CUSTOMER
            RestoreScope.BOTH -> CoreRestoreScope.BOTH
        }

        val inventoryDbPath = fileSystem.getDatabaseFile("inventory.db")
        val customerDbPath = fileSystem.getDatabaseFile("customer.db")

        val inventoryBakPath = fileSystem.getTempDir() / "inventory.db.bak"
        val customerBakPath = fileSystem.getTempDir() / "customer.db.bak"

        var rollbackRequired = false

        return try {
            // 1. Create atomic .bak rollback snapshots of existing production databases
            if (fileSystem.exists(inventoryDbPath)) {
                fileSystem.writeBytes(inventoryBakPath, fileSystem.readBytes(inventoryDbPath))
            }
            if (fileSystem.exists(customerDbPath)) {
                fileSystem.writeBytes(customerBakPath, fileSystem.readBytes(customerDbPath))
            }

            rollbackRequired = true

            // 2. Create unique isolated temporary workspace
            tempDir = fileSystem.createTempDirectory("restore_process")
            val decryptedArchive = tempDir / "archive.zip"

            // 3. Decrypt encrypted backup container into clear zip target
            crypto.decrypt(backupPath, decryptedArchive, password)

            // 4. Extract compressed contents to staging directory
            archive.extractArchive(decryptedArchive, tempDir)

            val manifestPath = tempDir / "manifest.json"

            // 5. Read and decode manifest metadata
            val jsonText = fileSystem.readText(manifestPath)
            val manifest = Json.decodeFromString<BackupManifest>(jsonText)

            validateManifest(manifest)

            // 6. Check integrity parameters across extracted databases
            val extractedInventory = tempDir / "inventory.db"
            val extractedCustomer = tempDir / "customer.db"
            val inventoryChecksum = if (fileSystem.exists(extractedInventory)) crypto.checksum(extractedInventory) else "empty"
            val customerChecksum = if (fileSystem.exists(extractedCustomer)) crypto.checksum(extractedCustomer) else "empty"

            val checksumPayloadPath = tempDir / "checksum_payload.txt"
            fileSystem.writeBytes(checksumPayloadPath, (inventoryChecksum + customerChecksum).encodeToByteArray())
            val localChecksum = crypto.checksum(checksumPayloadPath)
            fileSystem.delete(checksumPayloadPath)

            require(localChecksum == manifest.checksum) {
                "Backup corruption detected. Checksums do not match."
            }

            // 7. Structural Validation before closing operational database
            dbDriver.validateDatabases(tempDir, coreScope)

            // 8. CRITICAL: Close running engine driver to drop file locks
            dbDriver.close()

            // 9. Overwrite active database(s) with verified production backup file(s) based on scope
            dbDriver.importDatabases(tempDir, coreScope)

            // 10. Re-initialize database connections and step through any schema updates
            dbDriver.runMigrations()

            // 11. Run SQLite PRAGMA integrity check
            dbDriver.runIntegrityCheck(coreScope)

            // Success! No rollback needed
            rollbackRequired = false

            RestoreResult.Success
        } catch (e: Exception) {
            // Rollback on failure
            if (rollbackRequired) {
                try {
                    if (fileSystem.exists(inventoryBakPath)) {
                        fileSystem.writeBytes(inventoryDbPath, fileSystem.readBytes(inventoryBakPath))
                    }
                    if (fileSystem.exists(customerBakPath)) {
                        fileSystem.writeBytes(customerDbPath, fileSystem.readBytes(customerBakPath))
                    }
                    dbDriver.runMigrations() // Reinitialize engine
                } catch (rollbackEx: Exception) {
                    println("Critical: Database rollback failed: ${rollbackEx.message}")
                }
            }

            RestoreResult.Failure(
                message = e.message ?: "Restore routine broken",
                cause = e
            )
        } finally {
            tempDir?.let {
                try {
                    fileSystem.deleteRecursively(it)
                } catch (e: Exception) {
                    println("Failed to clean up restore staging directory: ${e.message}")
                }
            }
            try {
                fileSystem.delete(inventoryBakPath)
                fileSystem.delete(customerBakPath)
            } catch (_: Exception) {}
        }
    }

    private fun validateManifest(manifest: BackupManifest) {
        require(manifest.schemaVersion <= appInfo.databaseSchemaVersion) {
            "Downgrade prohibited! Backup version (${manifest.schemaVersion}) is higher than current application engine."
        }
    }
}
