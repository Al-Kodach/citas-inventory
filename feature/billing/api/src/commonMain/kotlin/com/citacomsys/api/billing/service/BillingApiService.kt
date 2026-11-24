package com.citacomsys.api.billing.service

import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.api.billing.PaymentSubmission
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest

interface BillingApiService {
    suspend fun createSmsTopupInvoice(packageId: String): BillingRecord

    suspend fun renewSubscription(billingTermId: String): BillingRecord
    suspend fun getBillById(invoiceId: String): BillingRecord

    suspend fun upgradeSubscription(request: PkgProvisionRequest): BillingRecord

    suspend fun getActiveInvoices(): Result<List<BillInvoice>>

    suspend fun getPaymentHistory(): Result<List<TransactionHistory>>

    suspend fun submitPayment(invoiceId: String, request: PaymentSubmission): Result<Unit>

    suspend fun cancelInvoice(invoiceId: String, reason: String): Result<Unit>
    suspend fun getInvoicePreview(type: PreviewRequest): InvoicePreviewUiState
}
