package com.citacomsys.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransactionWithReturn
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.citacomsys.core.database.util.ReentrantMutex
import com.citacomsys.customer.database.CustomerDatabase
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.getOrCreateSecret
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CustomerDatabaseManager {
    val database: Flow<CustomerDatabase>
    val dispatcher: CoroutineDispatcher
    suspend fun getDatabase(): CustomerDatabase
    suspend fun <T> dbQuery(block: suspend () -> T): T
    fun <T : Any> observeSingle(query: (CustomerDatabase) -> Query<T>): Flow<T?>
    fun <T : Any> observesList(query: (CustomerDatabase) -> Query<T>): Flow<List<T>>
    suspend fun <R> transactionWithResult(noEnclosing: Boolean = false, bodyWithReturn: suspend SuspendingTransactionWithReturn<R>.() -> R): R
    suspend fun transaction(noEnclosing: Boolean = false, body: suspend SuspendingTransactionWithoutReturn.() -> Unit)
}

internal class CustomerDatabaseManagerImpl(
    private val driverFactory: DriverFactory,
    private val vault: KSafe,
    override val dispatcher: CoroutineDispatcher
) : CustomerDatabaseManager {

    private val _database = MutableStateFlow<CustomerDatabase?>(null)
    private val initMutex = Mutex()
    private val writeMutex = ReentrantMutex()

    @OptIn(DelicateCoroutinesApi::class)
    override val database: Flow<CustomerDatabase> = _database
        .onStart {
            GlobalScope.launch(dispatcher) {
                getDatabase()
            }
        }
        .filterNotNull()

    override suspend fun getDatabase(): CustomerDatabase {
        return _database.value ?: initMutex.withLock {
            _database.value ?: run {
                Napier.d { "Lazy Initializing Encrypted CustomerDatabase..." }
                val secret = vault.getOrCreateSecret("customers.db")
                createCustomerDatabase(driverFactory, secret).also {
                    _database.value = it
                    Napier.i { "CustomerDatabase initialized successfully." }
                }
            }
        }
    }

    override suspend fun <T> dbQuery(block: suspend () -> T): T =
        withContext(dispatcher) { block() }

    override fun <T : Any> observeSingle(query: (CustomerDatabase) -> Query<T>): Flow<T?> =
        database.flatMapLatest { db -> query(db).asFlow().mapToOneOrNull(dispatcher) }

    override fun <T : Any> observesList(query: (CustomerDatabase) -> Query<T>): Flow<List<T>> =
        database.flatMapLatest { db -> query(db).asFlow().mapToList(dispatcher) }

    override suspend fun <R> transactionWithResult(
        noEnclosing: Boolean,
        bodyWithReturn: suspend SuspendingTransactionWithReturn<R>.() -> R
    ): R = writeMutex.withLock {
        val db = getDatabase()
        withContext(dispatcher) {
            db.transactionWithResult(noEnclosing, bodyWithReturn)
        }
    }

    override suspend fun transaction(
        noEnclosing: Boolean,
        body: suspend SuspendingTransactionWithoutReturn.() -> Unit
    ) = writeMutex.withLock {
        val db = getDatabase()
        withContext(dispatcher) {
            db.transaction(noEnclosing, body)
        }
    }
}
