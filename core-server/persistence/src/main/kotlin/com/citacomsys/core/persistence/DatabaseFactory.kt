package com.citacomsys.core.persistence

import com.citacomsys.core.persistence.config.DbConfig
import com.citacomsys.core.persistence.transaction.dbQuery
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger("CORE-PERSISTENCE")

class DatabaseFactory(
    val config: DbConfig,
    private val backupDir: File
) {

    private var _dataSource: HikariDataSource? = null
    val dataSource: HikariDataSource get() = _dataSource ?: error("Database not initialized")

    /**
     * Updated to be a suspend function to allow non-blocking initialization
     */
    suspend fun init() = withContext(Dispatchers.IO) {
        Class.forName("org.postgresql.Driver")

        logger.info { "Initializing database connection pool..." }
        val ds = createHikariDataSource()
        Database.connect(ds)
        _dataSource = ds

        val schemaLocation = "classpath:db/migration"
        val locationsToScan = if (config.isProduction) {
            listOf(schemaLocation)
        } else {
            listOf(schemaLocation, "classpath:db/seed")
        }

        val flyway = Flyway.configure()
            .dataSource(ds)
            .locations(*locationsToScan.toTypedArray())
            .baselineOnMigrate(true)
            .cleanDisabled(config.isProduction)
            .load()

        if (config.isProduction) {
            runProdMigrations(flyway)
        } else {
            runDevMigrations(flyway)
        }

        // Start the Database Event Listeners for Data, Operation, and Security channels
        PostgresNotificationListener(config).start()
        PostgresOperationNotificationListener(config).start()
        PostgresSecurityNotificationListener(config).start()
    }

    private suspend fun runProdMigrations(flyway: Flyway) {
        val pending = flyway.info().pending()
        if (pending.isEmpty()) {
            logger.info { "Database schema is up-to-date." }
            return
        }

        logger.info { "Found ${pending.size} pending migrations. Initiating background backup..." }

        // 1. Run the backup in the IO dispatcher
        val backupPath = createBackup()

        try {
            logger.info { "Backup completed at $backupPath. Proceeding with migration..." }

            // Flyway's migrate is blocking, but since we are in a suspend function
            // called via withContext(Dispatchers.IO), it won't block the main thread.
            flyway.migrate()

            logger.info { "Production migrations applied successfully." }
        } catch (e: Exception) {
            handleMigrationFailure(e, backupPath)
            throw IllegalStateException(
                "Critical: Prod migration failed. System rolled back to $backupPath",
                e
            )
        }
    }

    /**
     * Now a suspend function using Dispatchers.IO for native process execution
     */
    private suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        val (host, port, dbName) = parseDbUrl(config.url)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val backupFile = backupDir.resolve("pre_migration_${dbName}_$timestamp.dump")

        logger.info { "Executing pg_dump for $dbName..." }

        runSystemCommand(
            "pg_dump", "-h", host, "-p", port, "-U", config.user,
            "-F", "c", "-f", backupFile.absolutePath, dbName
        )

        backupFile.absolutePath
    }

    private suspend fun runSystemCommand(vararg command: String) = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(*command).apply {
            environment()["PGPASSWORD"] = config.pass
            redirectErrorStream(true)
        }.start()

        // Read output in a non-blocking way relative to the main thread
        val output = process.inputStream.bufferedReader().use { it.readText() }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException(
                "Command failed with exit code $exitCode: ${
                    command.joinToString(
                        " "
                    )
                }\nOutput: $output"
            )
        }
    }

    /**
     * Defense in Depth: Automated native maintenance
     * This can be called by MaintenanceJobFactory
     */
    suspend fun runNativeMaintenance() = withContext(Dispatchers.IO) {
        try {
            transaction {
                logger.info { "Executing native SIEM maintenance procedure..." }
                this.exec("CALL purge_expired_siem_logs();")
            }
        } catch (e: Exception) {
            logger.error(e) { "Native maintenance procedure failed" }
        }
    }

    private fun runDevMigrations(flyway: Flyway) {
        try {
            flyway.repair()
            flyway.migrate()
        } catch (e: Exception) {
            if (!config.isProduction) {
                logger.warn { "Dev migration mismatch: Cleaning and recreating..." }
                flyway.clean()
                flyway.migrate()
            }
        }
    }


    private suspend fun handleMigrationFailure(originalError: Exception, backupPath: String) {
        logger.error { "MIGRATION FAILED! Initiating emergency rollback..." }
        try {
            // Close pool to release locks before drop/restore
            _dataSource?.close()

            dropDatabase()
            createDatabase()
            restoreFromBackup(backupPath)

            logger.info { "Rollback successful. Database restored to state: $backupPath" }
        } catch (rollbackError: Exception) {
            logger.error(rollbackError) { "ROLLBACK FAILED! Manual intervention required." }
        }
    }

    private fun createHikariDataSource(): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.pass
            maximumPoolSize = config.poolSize
            driverClassName = "org.postgresql.Driver"

            // Optimization for high-concurrency SaaS
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

            if (config.isProduction) {
                isAutoCommit = false
                connectionTimeout = 30000
                leakDetectionThreshold = 60000
            }
        }
        return HikariDataSource(hikariConfig)
    }


    private suspend fun dropDatabase() {
        val (host, port, dbName) = parseDbUrl(config.url)
        runSystemCommand("dropdb", "-h", host, "-p", port, "-U", config.user, "--if-exists", dbName)
    }

    private suspend fun createDatabase() {
        val (host, port, dbName) = parseDbUrl(config.url)
        runSystemCommand("createdb", "-h", host, "-p", port, "-U", config.user, dbName)
    }

    private suspend fun restoreFromBackup(backupPath: String) {
        val (host, port, dbName) = parseDbUrl(config.url)
        runSystemCommand(
            "pg_restore",
            "-h",
            host,
            "-p",
            port,
            "-U",
            config.user,
            "-d",
            dbName,
            "-c",
            backupPath
        )
    }

    private fun parseDbUrl(url: String): Triple<String, String, String> {
        val pattern = "jdbc:postgresql://([^:]+):(\\d+)/(.+)".toRegex()
        val match = pattern.find(url) ?: throw IllegalStateException("Malformed JDBC URL: $url")
        return Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
    }
}
