package com.citacomsys.feature.billing.repository

import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.api.billing.service.BillingApiService
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.api.billing.PaymentSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface BillingRepository {
    val isProcessing: StateFlow<Boolean>

    /**
     * Fetches the current pending or submitted invoice for this pharmacy.
     */
    suspend fun getActiveInvoices(): Result<List<BillInvoice>>

    suspend fun getPaymentHistory(): Result<List<TransactionHistory>>

    /**
     * Informs the platform that payment has been made via MoMo/Cash,
     * providing the Transaction ID and Payer Name for verification.
     */
    suspend fun submitPayment(submission: PaymentSubmission): Result<Unit>

    suspend fun cancelInvoice(invoiceId: String, reason: String): Result<Unit>
}

internal class BillingRepositoryImpl(private val api: BillingApiService) : BillingRepository {

    private val _isProcessing = MutableStateFlow(false)
    override val isProcessing = _isProcessing.asStateFlow()

    override suspend fun getActiveInvoices(): Result<List<BillInvoice>> = runCatching {
        api.getActiveInvoices().getOrThrow()
    }

    override suspend fun getPaymentHistory(): Result<List<TransactionHistory>> {
        _isProcessing.value = true
        return api.getPaymentHistory()
            .also { _isProcessing.value = false }
    }

    override suspend fun submitPayment(submission: PaymentSubmission): Result<Unit> {
        _isProcessing.value = true
        return api.submitPayment(submission.invoiceId, submission)
            .also { _isProcessing.value = false }
    }

    override suspend fun cancelInvoice(
        invoiceId: String,
        reason: String
    ): Result<Unit> {
        _isProcessing.value = true
        return api.cancelInvoice(invoiceId, reason)
            .also { _isProcessing.value = false }
    }
}