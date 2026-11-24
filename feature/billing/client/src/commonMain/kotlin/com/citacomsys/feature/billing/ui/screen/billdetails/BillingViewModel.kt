package com.citacomsys.feature.billing.ui.screen.billdetails

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.api.billing.PaymentSubmission
import com.citacomsys.api.billing.service.BillingApiService
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingDetailsState(
    val billRecord: UiResult<BillingRecord> = UiResult.Idle,
    val preview: UiResult<InvoicePreviewUiState> = UiResult.Idle,
    val actionState: UiResult<Unit> = UiResult.Idle
)

class BillingViewModel(
    private val billingApiService: BillingApiService,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    private val _state = MutableStateFlow(BillingDetailsState())
    val state: StateFlow<BillingDetailsState> = _state.asStateFlow()

    /**
     * Fetch a specific invoice by its ID
     */
    fun loadBillDetails(billId: String) {
        viewModelScope.launch {
            _state.update { it.copy(billRecord = UiResult.Loading) }
            val result = runCatching {
                billingApiService.getBillById(billId)
            }.asUiResult()
            _state.update { it.copy(billRecord = result) }
        }
    }


    /**
     * Action: Cancel a pending bill
     */
    fun cancelBill(invoiceId: String, reason: String, onSuccess: () -> Unit) {
        executeAction("Bill Canceled") {
            billingApiService.cancelInvoice(invoiceId, reason)
            onSuccess()
        }
    }

    /**
     * Action: Mark bill as paid (Internal/Admin or via Payment Callback)
     */
    fun markAsPaid(
        invoiceId: String,
        senderName: String,
        transactionId: String,
        onSuccess: () -> Unit
    ) {
        val request = PaymentSubmission(
            invoiceId = invoiceId,
            payerName = senderName,
            transactionId = transactionId
        )
        executeAction("Payment Confirmed") {
            billingApiService.submitPayment(invoiceId, request)
            onSuccess()
        }
    }

    private fun executeAction(
        successMessage: String,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(actionState = UiResult.Loading) }
            runCatching { block() }
                .onSuccess {
                    _state.update { it.copy(actionState = UiResult.Success(Unit)) }
                    showSuccess(successMessage)
                }.onFailure { e ->
                    _state.update { it.copy(actionState = e.asUiError()) }
                    showError(e.message ?: "Action failed")
                }
        }
    }
}