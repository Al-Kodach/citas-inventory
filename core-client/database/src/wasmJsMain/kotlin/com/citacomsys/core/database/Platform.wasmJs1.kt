package com.citacomsys.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class DriverFactory {
     fun createDriver(): SqlDriver {
        TODO("Not yet implemented")
    }

    actual suspend fun createNamedDriver(
        name: String,
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        passphrase: ByteArray?
    ): SqlDriver {
        TODO("Not yet implemented")
    }
}
