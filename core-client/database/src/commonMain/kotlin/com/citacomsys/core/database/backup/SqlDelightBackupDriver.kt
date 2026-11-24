package com.citacomsys.core.database.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.citacomsys.core.domain.file.PlatformFileSystem
import com.citacomsys.database.InventoryDatabase
import io.github.aakira.napier.Napier
import okio.Path

internal class SqlDelightBackupDriver(
    private val fs: PlatformFileSystem,
    private var driver: SqlDriver,
) : DatabaseBackupDriver {

    private val inventoryDbPath: Path = fs.getDatabaseFile("inventory.db")
    private val customerDbPath: Path = fs.getDatabaseFile("customer.db")

    override suspend fun getDatabaseMetaInfo(): DatabaseMetaData {
        val schemaVersion = InventoryDatabase.Schema.version.toInt()
        return DatabaseMetaData(
            currentVersion = getCurrentVersion(),
            schemaVersion = schemaVersion
        )
    }

    override suspend fun exportDatabases(tempDir: Path) {
        // To guarantee zero active thread modifications, force SQLite checkpoint
        try {
            driver.execute(null, "PRAGMA wal_checkpoint(FULL);", 0)
        } catch (e: Exception) {
            Napier.w(e) { "Failed to checkpoint inventory db during export" }
        }

        if (fs.exists(inventoryDbPath)) {
            val bytes = fs.readBytes(inventoryDbPath)
            fs.writeBytes(tempDir / "inventory.db", bytes)
        }
        if (fs.exists(customerDbPath)) {
            val bytes = fs.readBytes(customerDbPath)
            fs.writeBytes(tempDir / "customer.db", bytes)
        }
    }

    override suspend fun importDatabases(tempDir: Path, scope: RestoreScope) {
        if (scope == RestoreScope.INVENTORY || scope == RestoreScope.BOTH) {
            val sourceInventory = tempDir / "inventory.db"
            if (fs.exists(sourceInventory)) {
                val bytes = fs.readBytes(sourceInventory)
                fs.writeBytes(inventoryDbPath, bytes)
            }
        }
        if (scope == RestoreScope.CUSTOMER || scope == RestoreScope.BOTH) {
            val sourceCustomer = tempDir / "customer.db"
            if (fs.exists(sourceCustomer)) {
                val bytes = fs.readBytes(sourceCustomer)
                fs.writeBytes(customerDbPath, bytes)
            }
        }
    }

    override suspend fun close() {
        try {
            driver.close()
        } catch (e: Exception) {
            Napier.w(e) { "Failed to close driver" }
        }
    }

    override suspend fun validateDatabases(tempDir: Path, scope: RestoreScope) {
        if (scope == RestoreScope.INVENTORY || scope == RestoreScope.BOTH) {
            val path = tempDir / "inventory.db"
            validateSingleDatabase(path)
        }
        if (scope == RestoreScope.CUSTOMER || scope == RestoreScope.BOTH) {
            val path = tempDir / "customer.db"
            validateSingleDatabase(path)
        }
    }

    override suspend fun runIntegrityCheck(scope: RestoreScope) {
        if (scope == RestoreScope.INVENTORY || scope == RestoreScope.BOTH) {
            runIntegrityCheckOnDriver(driver)
        }
        // For customer.db, integrity check can be executed if needed
    }

    private fun runIntegrityCheckOnDriver(sqlDriver: SqlDriver) {
        val result = sqlDriver.executeQuery(
            identifier = null,
            sql = "PRAGMA integrity_check;",
            mapper = { cursor ->
                if (cursor.next().value) {
                    QueryResult.Value(cursor.getString(0) ?: "error")
                } else {
                    QueryResult.Value("no_result")
                }
            },
            parameters = 0
        )
        require(result.value == "ok") {
            "Database integrity check failed: ${result.value}"
        }
    }

    private suspend fun validateSingleDatabase(path: Path) {
        require(fs.exists(path)) { "Required database file at $path is missing from backup archive." }
        val bytes = fs.readBytes(path)
        require(bytes.size > 100) { "Database file at $path is too small or corrupted." }

        // Assert SQLite Magic Header Signature
        val fileHeader = bytes.take(16).map { it.toInt().toChar() }.joinToString("")
        require(fileHeader.startsWith("SQLite format 3")) {
            "Target validation asset at $path is not a structurally sound SQLite format 3 data file."
        }
    }

    override suspend fun runMigrations() {
        // Use user_version mechanism to inspect database migration profiles dynamically
        val currentVersion = getCurrentVersion()
        val schemaVersion = InventoryDatabase.Schema.version.toInt()

        if (currentVersion < schemaVersion) {
            Napier.i { "Migrating inventory database from v$currentVersion to v$schemaVersion..." }
            InventoryDatabase.Schema.migrate(
                driver,
                currentVersion.toLong(),
                schemaVersion.toLong()
            ).await()
            driver.setVersion(schemaVersion)
            Napier.i { "Migration completed successfully." }
        } else if (currentVersion > schemaVersion) {
            Napier.w { "Database version ($currentVersion) is newer than schema version ($schemaVersion)." }
        } else {
            Napier.i { "Database is up to date." }
        }
    }

    private fun SqlDriver.setVersion(version: Int) {
        this.execute(null, "PRAGMA user_version = $version;", 0, null)
    }

    private fun getCurrentVersion(): Int {
        val result = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor ->
                if (cursor.next().value) {
                    QueryResult.Value(cursor.getLong(0)?.toInt() ?: 0)
                } else {
                    QueryResult.Value(0)
                }
            },
            parameters = 0
        )
        return result.value
    }

}
