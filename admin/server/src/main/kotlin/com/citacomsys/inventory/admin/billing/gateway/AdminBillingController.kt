package com.citacomsys.inventory.admin.billing.gateway

import com.citacomsys.api.billing.service.BillingService
import com.citacomsys.core.persistence.transaction.readTransaction
import com.citacomsys.core.persistence.transaction.writeTransaction
import com.citacomsys.core.security.plugin.actionPrincipal
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond

internal class AdminBillingController(
    private val billingService: BillingService,
) {

    suspend fun fetch(call: ApplicationCall) {
        val invoices = call.readTransaction { billingService.fetch() }
        call.respond(invoices)
    }

    suspend fun getRecord(call: ApplicationCall) {
        val id = call.getId()
        val record = call.readTransaction { billingService.get(id) }
        call.respond(record)
    }

    suspend fun verified(call: ApplicationCall) {
        val principal = call.actionPrincipal()
        val ref = call.getRef()

        call.writeTransaction {
            billingService.verify(
                reference = ref,
                userId = principal.userId,
                sessionId = principal.sessionId
            ).getOrThrow()
        }

        call.respond(Unit)
    }

    suspend fun reject(call: ApplicationCall) {
        val principal = call.actionPrincipal()
        val id = call.parameters["id"] ?: error("Invoice batchId is missing")
        val reason = call.receiveText()

        call.writeTransaction {
            billingService.rejectInvoice(
                invoiceId = id,
                reason = reason,
                sessionId = principal.sessionId
            ).getOrThrow()
        }

        call.respond(Unit)
    }

    private fun ApplicationCall.getId() = parameters["id"] ?: error("Invoice batchId is missing")
    private fun ApplicationCall.getRef() = parameters["ref"] ?: error("Reference code is missing")
}