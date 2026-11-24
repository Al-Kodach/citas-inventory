package com.citacomsys.test.server

import com.citacomsys.core.persistence.transaction.TransactionManager
import io.mockk.coEvery
import io.mockk.mockk

object MockDatabaseUtils {
    fun createMockTransactionManager(): TransactionManager {
        val tm = mockk<TransactionManager>(relaxed = true)
        coEvery { tm.runInTransaction(any<suspend () -> Any>()) } answers {
            val block = it.invocation.args[0] as suspend () -> Any
            runBlocking { block() }
        }
        coEvery { tm.runReadOnly(any<suspend () -> Any>()) } answers {
            val block = it.invocation.args[0] as suspend () -> Any
            runBlocking { block() }
        }
        return tm
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}
