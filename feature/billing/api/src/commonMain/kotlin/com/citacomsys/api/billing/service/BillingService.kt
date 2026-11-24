package com.citacomsys.api.billing.service

import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.api.billing.PaymentSubmission
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.model.ProvisionedPackage
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest

interface BillingService {
    val serviceId: String get() = "billing_service"

    suspend fun fetch(): List<BillInvoice>
    suspend fun get(id: String): BillingRecord

    suspend fun createTrailConventionInvoice(
        workspaceId: String,
        provisionedPackage: ProvisionedPackage
    ): Unit

    suspend fun createSmsTopupInvoice(
        workspaceId: String,
        packageId: String,
        userId: String,
        sessionId: String
    ): BillingRecord

    suspend fun createRenewalInvoice(
        workspaceId: String,
        userId: String,
        sessionId: String
    ): BillingRecord

    suspend fun createUpgradeInvoice(
        workspaceId: String,
        request: PkgProvisionRequest,
        userId: String,
        sessionId: String?,
    ): BillingRecord

    suspend fun paymentSubmitted(
        info: PaymentSubmission,
        workspaceId: String,
        userId: String,
        sessionId: String
    )

    suspend fun invoicePreviewRequest(request: PreviewRequest): InvoicePreviewUiState

    suspend fun getPaymentHistory(workspaceId: String): List<TransactionHistory>

    suspend fun cancelInvoice(
        invoiceId: String,
        workspaceId: String,
        sessionId: String,
        reason: String
    ): Result<Unit>

    suspend fun verify(
        reference: String,
        userId: String,
        sessionId: String
    ): Result<Unit>

    suspend fun rejectInvoice(
        invoiceId: String,
        reason: String,
        sessionId: String,
    ): Result<Unit>

    suspend fun ensureRenewalInvoice(workspaceId: String): BillingRecord?

    suspend fun ensureOnboardingInvoice(workspaceId: String): BillingRecord?

    suspend fun getActiveInvoices(workspaceId: String): List<BillingRecord>
    suspend fun getInvoiceById(invoiceId: String, workspaceId: String): BillingRecord
}
