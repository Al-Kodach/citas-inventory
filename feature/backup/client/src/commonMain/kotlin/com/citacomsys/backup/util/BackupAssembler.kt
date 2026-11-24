package com.citacomsys.backup.util

import com.citacomsys.backup.api.BackupBytesResult
import com.citacomsys.backup.api.BackupResult
import com.citacomsys.backup.api.fileUtil.BackupArchiveService
import com.citacomsys.backup.api.fileUtil.BackupCryptoService
import com.citacomsys.backup.api.model.BackupManifest
import com.citacomsys.core.database.backup.DatabaseBackupDriver
import com.citacomsys.core.domain.AppInfoProvider
import com.citacomsys.core.domain.file.PlatformFileSystem
import com.citacomsys.core.domain.service.AppSessionManager
import kotlinx.serialization.json.Json
import kotlin.time.Clock

internal class BackupAssembler(
    private val dbDriver: DatabaseBackupDriver,
    private val crypto: BackupCryptoService,
    private val archive: BackupArchiveService,
    private val appInfoProvider: AppInfoProvider,
    private val fs: PlatformFileSystem,
    private val sessionManager: AppSessionManager
) {
    suspend fun assemble(password: String, exportName: String): BackupResult {
        // Create a unique staging directory inside the isolated system temporary path
        val tempStageDir =
            fs.getTempDir() / "backup_stage_${Clock.System.now().toEpochMilliseconds()}"
        val manifestPath = tempStageDir / "manifest.json"
        val zipOutputPath = fs.getTempDir() / "backup.zip"

        val sanitizedExtension = if (exportName.endsWith(".citasbackup")) "" else ".citasbackup"
        val finalPath = fs.getInternalDir() / "$exportName$sanitizedExtension"

        return try {
            // 1. Export both inventory.db and customer.db into staging directory
            dbDriver.exportDatabases(tempStageDir)

            val inventoryDbPath = tempStageDir / "inventory.db"
            val customerDbPath = tempStageDir / "customer.db"
            val inventoryChecksum = if (fs.exists(inventoryDbPath)) crypto.checksum(inventoryDbPath) else "empty"
            val customerChecksum = if (fs.exists(customerDbPath)) crypto.checksum(customerDbPath) else "empty"

            val checksumPayloadPath = tempStageDir / "checksum_payload.txt"
            fs.writeBytes(checksumPayloadPath, (inventoryChecksum + customerChecksum).encodeToByteArray())
            val combinedChecksum = crypto.checksum(checksumPayloadPath)
            fs.delete(checksumPayloadPath)

            val user = sessionManager.appUser.value

            // 2. Generate manifest payload
            val manifest = BackupManifest(
                backupVersion = 1,
                schemaVersion = appInfoProvider.databaseSchemaVersion,
                appVersion = appInfoProvider.appVersion,
                createdAt = Clock.System.now(),
                createdBy = user?.name ?: "Offline_System",
                checksum = combinedChecksum,
                timestamp = Clock.System.now()
            )

            val jsonText = Json.encodeToString(BackupManifest.serializer(), manifest)
            fs.writeBytes(manifestPath, jsonText.encodeToByteArray())

            // 3. Package staging directory up into a standard Zip Archive
            archive.createArchive(tempStageDir, zipOutputPath)

            // Encrypt the zip archive with AES and clean up staging
            crypto.encrypt(zipOutputPath, finalPath, password)

            BackupResult.Success(
                path = finalPath.toString(),
                fileSize = fs.getFileSize(finalPath)
            )
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Backup assembly process failed")
        } finally {
            // Ensure temp files never linger to exhaust disk space
            fs.deleteRecursively(tempStageDir)
            fs.delete(zipOutputPath)
        }
    }

    suspend fun assembleToBytes(password: String): BackupBytesResult {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val tempStageDir = fs.getTempDir() / "backup_stage_$timestamp"
        val manifestPath = tempStageDir / "manifest.json"
        val zipOutputPath = fs.getTempDir() / "backup.zip"
        val encryptedOutputPath = fs.getTempDir() / "backup.citabackup"

        return try {
            // 1. Export both databases safely
            dbDriver.exportDatabases(tempStageDir)

            val inventoryDbPath = tempStageDir / "inventory.db"
            val customerDbPath = tempStageDir / "customer.db"
            val inventoryChecksum = if (fs.exists(inventoryDbPath)) crypto.checksum(inventoryDbPath) else "empty"
            val customerChecksum = if (fs.exists(customerDbPath)) crypto.checksum(customerDbPath) else "empty"

            val checksumPayloadPath = tempStageDir / "checksum_payload.txt"
            fs.writeBytes(checksumPayloadPath, (inventoryChecksum + customerChecksum).encodeToByteArray())
            val combinedChecksum = crypto.checksum(checksumPayloadPath)
            fs.delete(checksumPayloadPath)

            val user = sessionManager.appUser.value

            // 2. Generate security manifest payload
            val manifest = BackupManifest(
                backupVersion = 1,
                schemaVersion = appInfoProvider.databaseSchemaVersion,
                appVersion = appInfoProvider.appVersion,
                createdAt = Clock.System.now(),
                createdBy = user?.name ?: "Universal_Client",
                checksum = combinedChecksum,
                timestamp = Clock.System.now()
            )
            fs.writeBytes(
                manifestPath,
                Json.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray()
            )

            // 3. Archive files into intermediate container
            archive.createArchive(tempStageDir, zipOutputPath)

            // 4. Encrypt zip file with AES-GCM
            crypto.encrypt(zipOutputPath, encryptedOutputPath, password)

            // 5. Read back out to memory bytes array to dispatch out of sandbox
            val finalBytes = fs.readBytes(encryptedOutputPath)
            BackupBytesResult.Success(finalBytes)

        } catch (e: Exception) {
            BackupBytesResult.Failure(e.message ?: "ByteArray assembly failure")
        } finally {
            // Guarantee isolated temporary resources are scrubbed from physical drive arrays
            fs.deleteRecursively(tempStageDir)
            fs.delete(zipOutputPath)
            fs.delete(encryptedOutputPath)
        }
    }
}
