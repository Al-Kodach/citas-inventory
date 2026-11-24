package com.citacomsys.core.persistence.test

import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.table.tenant.WorkstationBindings
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val logger = KotlinLogging.logger { }

object InMemoryDatabaseFactory {

    fun init() {
        Database.connect(hikari())
        createSchemaForTests()
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.h2.Driver"
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    /**
     * Creates the database schema directly using Exposed's SchemaUtils.
     * This is much faster than running full Flyway migrations for every test.
     */
    fun createSchemaForTests() {
        transaction {
            logger.info { "Dropping and creating tables for test run..." }
            SchemaUtils.create(
                HardwareRegistry,
                WorkstationBindings
            )
            logger.info { "Test schema created successfully." }
        }
    }
}