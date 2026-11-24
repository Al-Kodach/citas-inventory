package com.citacomsys.inventory.admin.billing.remote

import com.citacomsys.admin.api.billing.AdminBillingApiService
import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class KtorAdminBillingServiceApi(
    client: HttpClient,
    private val endpoint: String = "api/v1/admin/billing"
) : AdminBillingApiService, BaseApi(client) {

    override suspend fun fetch(): List<BillInvoice> {
        return executeRequest { client.get(endpoint) }
    }

    override suspend fun getRecord(id: String): BillingRecord {
        return executeRequest { client.get("$endpoint/$id") }
    }

    override suspend fun verified(reference: String) {
        return executeRequest { client.post("$endpoint/$reference/verified") }
    }

    override suspend fun reject(invoiceId: String, reason: String) {
        return executeRequest {
            client.post("$endpoint/$invoiceId/reject") {
                setBody(reason)
            }
        }
    }
}