package com.citacomsys.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransactionWithReturn
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.citacomsys.database.InventoryDatabase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.Koin
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

interface InventoryDatabaseManager {
    /**
     * Reactive flow of the database instance.
     * Emits when the database is fully initialized and migrated.
     */
    val database: Flow<InventoryDatabase>

    val dispatcher: CoroutineDispatcher

    /**
     * Synchronously (via suspend) waits for and returns the database instance.
     */
    suspend fun getDatabase(): InventoryDatabase

    suspend fun <T> dbQuery(block: suspend () -> T): T

    /**
     * Helper to observe a single query result reactively.
     */
    fun <T : Any> observeSingle(query: (InventoryDatabase) -> Query<T>): Flow<T?>

    /**
     * Helper to observe a list query result reactively.
     */
    fun <T : Any> observesList(
        query: (InventoryDatabase) -> Query<T>
    ): Flow<List<T>>

    suspend fun <R> transactionWithResult(
        noEnclosing: Boolean = false,
        bodyWithReturn: suspend SuspendingTransactionWithReturn<R>.() -> R
    ): R

    suspend fun transaction(
        noEnclosing: Boolean = false,
        body: suspend SuspendingTransactionWithoutReturn.() -> Unit
    )
}

internal class InventoryDatabaseManagerImpl(
    private val driverFactory: DriverFactory,
    private val koin: Koin,
    override val dispatcher: CoroutineDispatcher
) : InventoryDatabaseManager {

    private val _database = MutableStateFlow<InventoryDatabase?>(null)

    @OptIn(DelicateCoroutinesApi::class)
    override val database: Flow<InventoryDatabase> = _database
        .onStart {
            // Trigger lazy init if not already started
            GlobalScope.launch(dispatcher) {
                getDatabase()
            }
        }
        .filterNotNull()

    private val initMutex = Mutex()
    private val writeMutex = com.citacomsys.core.database.util.ReentrantMutex()

    override suspend fun getDatabase(): InventoryDatabase {
        return _database.value ?: initMutex.withLock {
            _database.value ?: run {
                Napier.d { "Background Initializing InventoryDatabase..." }
                createCoreDatabase(driverFactory, koin).also {
                    _database.value = it
                    koin.loadModules(listOf(module { single { it } }))
                    Napier.i { "InventoryDatabase initialized successfully." }
                }
            }
        }
    }

    override suspend fun <T> dbQuery(block: suspend () -> T): T =
        withContext(dispatcher) {
            block()
        }

    override fun <T : Any> observeSingle(query: (InventoryDatabase) -> Query<T>): Flow<T?> =
        database.flatMapLatest { db -> query(db).asFlow().mapToOneOrNull(dispatcher) }

    override fun <T : Any> observesList(
        query: (InventoryDatabase) -> Query<T>
    ): Flow<List<T>> =
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
