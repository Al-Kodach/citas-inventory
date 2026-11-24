package com.citacomsys.core.persistence.transaction

import com.citacomsys.core.persistence.plugin.transactionManager
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.sql.Connection
import kotlin.coroutines.CoroutineContext

class TransactionManager {
    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return dbQuery {
            withContext(TransactionContext(this)) {
                block()
            }
        }
    }

    suspend fun <T> runReadOnly(block: suspend () -> T): T {
        return suspendTransaction(
            readOnly = true,
            transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
        ) {
            withContext(TransactionContext(this)) {
                block()
            }
        }
    }
}

class TransactionContext(val transaction: Transaction) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<TransactionContext>

    override val key: CoroutineContext.Key<*> = Key
}

suspend fun currentTransaction(): Transaction {
    return currentCoroutineContext()[TransactionContext.Key]?.transaction
        ?: throw IllegalStateException("This operation must be run within a transaction.")
}


suspend inline fun <reified T> ApplicationCall.readTransaction(crossinline block: suspend () -> T): T {
    return transactionManager.runReadOnly { block() }
}

suspend inline fun <reified T> ApplicationCall.writeTransaction(crossinline block: suspend () -> T): T {
    return transactionManager.runInTransaction { block() }
}