package com.citacomsys.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.citacomsys.core.domain.file.PlatformFileSystem
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class DriverFactory(private val fs: PlatformFileSystem) {
    private val lock = Mutex()

    actual suspend fun createNamedDriver(
        name: String,
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        passphrase: ByteArray?
    ): SqlDriver {
        val path = fs.getInternalDir().resolve(name).toString()
        return setupDriver(path, schema, passphrase)
    }

    private suspend fun setupDriver(
        path: String,
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        passphrase: ByteArray? = null
    ): SqlDriver {
        Napier.i { "Initializing database at path: $path (Encrypted: ${passphrase != null})" }

        // Ensure parent directory exists before SQLite tries to open the file
        File(path).parentFile?.mkdirs()

        val connectionUrl = "jdbc:sqlite:$path"

        val driver = JdbcSqliteDriver(connectionUrl)
        if (passphrase != null) {
            val hexKey = passphrase.joinToString("") { "%02x".format(it) }
            driver.execute(null, "PRAGMA key = '$hexKey';", 0, null)
        }
        driver.execute(null, "PRAGMA journal_mode=WAL;", 0, null)
        driver.execute(null, "PRAGMA busy_timeout=5000;", 0, null)
        driver.execute(null, "PRAGMA auto_vacuum=FULL;", 0, null)

        lock.withLock {
            try {
                val currentVersion = driver.getCurrentVersion()
                val schemaVersion = schema.version.toInt()

                Napier.i { "Database ($path) - Found version: $currentVersion, Schema version: $schemaVersion" }

                if (currentVersion == 0) {
                    Napier.i { "Creating new database schema for $path..." }
                    schema.create(driver).await()
                    driver.setVersion(schemaVersion)
                    Napier.i { "Database created successfully." }
                } else {
                    migrate(driver, schema, schemaVersion)
                }
            } catch (e: Exception) {
                Napier.e(e) { "FATAL: Database schema management failed for $path." }
                throw IllegalStateException("Database setup failed. File may be corrupt at $path", e)
            }
        }

        return driver
    }

    private suspend fun migrate(
        driver: SqlDriver,
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        schemaVersion: Int
    ) {
        val currentVersion = driver.getCurrentVersion()

        if (currentVersion < schemaVersion) {
            Napier.i { "Migrating database from v$currentVersion to v$schemaVersion..." }
            schema.migrate(
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
}

private fun SqlDriver.getCurrentVersion(): Int {
    val result = this.executeQuery(
        identifier = null,
        "PRAGMA user_version;",
        mapper = { cursor ->
            if (cursor.next().value) QueryResult.Value(
                cursor.getLong(0)?.toInt() ?: 0
            ) else QueryResult.Value(0)
        },
        parameters = 0,
        binders = null
    )

    return result.value
}

private fun SqlDriver.setVersion(version: Int) {
    this.execute(null, "PRAGMA user_version = $version;", 0, null)
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
}
