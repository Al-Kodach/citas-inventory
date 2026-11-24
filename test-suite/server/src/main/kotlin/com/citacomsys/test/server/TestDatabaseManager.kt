package com.citacomsys.test.server

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TestDatabaseManager(private val tables: List<Table>) {

    init {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    }

    fun setupSchema() {
        transaction {
            SchemaUtils.create(*tables.toTypedArray())
        }
    }

    fun clearTables() {
        transaction {
            SchemaUtils.drop(*tables.toTypedArray())
        }
    }
}
