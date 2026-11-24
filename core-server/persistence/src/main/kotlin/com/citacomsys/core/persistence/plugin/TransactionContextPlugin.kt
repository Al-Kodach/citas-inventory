package com.citacomsys.core.persistence.plugin

import com.citacomsys.core.persistence.transaction.TransactionManager
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.routing.RoutingCall
import io.ktor.util.AttributeKey
import kotlin.time.Instant

val TransactionManagerKey = AttributeKey<TransactionManager>("TransactionManager")

val Transactional = createApplicationPlugin(name = "Transactional") {
    val transactionManager = TransactionManager()
    onCall { call ->
        call.attributes.put(TransactionManagerKey, transactionManager)
    }
}

fun ApplicationCall.getChangeListParameter(): Instant {
    val since = this.request.queryParameters["since"]
    return since?.let { Instant.parse(it) } ?: Instant.parse("1990-01-01T00:00:00Z")
}

val ApplicationCall.transactionManager: TransactionManager
    get() = attributes[TransactionManagerKey]

suspend inline fun <reified T> RoutingCall.writeTransaction(crossinline action: suspend () -> T): T {
    return this.transactionManager.runInTransaction {
        action()
    }
}

suspend inline fun <reified T> RoutingCall.readTransaction(crossinline action: suspend () -> T): T {
    return this.transactionManager.runReadOnly {
        action()
    }
}