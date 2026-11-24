package com.citacomsys.core.database

import net.sqlcipher.database.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.sqlcipher.database.SupportFactory


@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class DriverFactory(private val context: android.content.Context) {
    actual suspend fun createNamedDriver(
        name: String,
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        passphrase: ByteArray?
    ): SqlDriver {
        // Initialize SQLCipher binary components
        SQLiteDatabase.loadLibs(context)

        // Pass encryption key via SupportFactory
        val factory = passphrase?.let {
            SupportFactory(it)
        }

        val callback = object : AndroidSqliteDriver.Callback(schema.synchronous()) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                super.onConfigure(db)
                db.execSQL("PRAGMA auto_vacuum = FULL;")
            }
        }

        return if (factory != null) {
            AndroidSqliteDriver(
                schema = schema.synchronous(),
                context = context,
                name = name,
                factory = factory,
                callback = callback
            )
        } else {
            AndroidSqliteDriver(
                schema = schema.synchronous(),
                context = context,
                name = name,
                callback = callback
            )
        }
    }
}
