package com.citacomsys.feature.billing.remote

import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.api.billing.service.BillingApiService
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.api.billing.PaymentSubmission
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class KtorBillingApiService(
    client: HttpClient,
    private val endpoint: String = "api/v1/billing"
) : BaseApi(client), BillingApiService {

    override suspend fun createSmsTopupInvoice(packageId: String): BillingRecord {
        return executeRequest { client.post("$endpoint/sms-topup/$packageId") }
    }

    override suspend fun renewSubscription(billingTermId: String): BillingRecord {
        return executeRequest { client.post("$endpoint/renewal/$billingTermId") }
    }

    override suspend fun getBillById(invoiceId: String): BillingRecord {
        return executeRequest { client.get("$endpoint/$invoiceId") }
    }

    override suspend fun upgradeSubscription(request: PkgProvisionRequest): BillingRecord {
        return executeRequest { client.post("$endpoint/upgrade") { setBody(request) } }
    }

    override suspend fun getActiveInvoices(): Result<List<BillInvoice>> = runCatching {
        executeRequest { client.get("$endpoint/active") }
    }

    override suspend fun getPaymentHistory(): Result<List<TransactionHistory>> = runCatching {
        executeRequest { client.get("$endpoint/history") }
    }


    override suspend fun submitPayment(
        invoiceId: String,
        request: PaymentSubmission
    ): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/$invoiceId/submit-payment") {
                setBody(request)
            }
        }
    }

    override suspend fun cancelInvoice(invoiceId: String, reason: String): Result<Unit> =
        runCatching {
            executeRequest {
                client.post("$endpoint/cancel/$invoiceId") {
                    setBody(reason)
                }
            }
        }

    override suspend fun getInvoicePreview(type: PreviewRequest): InvoicePreviewUiState {
        return executeRequest { client.post("$endpoint/previewInvoice") { setBody(type) } }
    }
}
