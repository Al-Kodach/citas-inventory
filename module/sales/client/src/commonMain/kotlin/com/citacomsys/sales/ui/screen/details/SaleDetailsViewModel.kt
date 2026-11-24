package com.citacomsys.sales.ui.screen.details

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.module.api.customer.CustomerLedgerEntry
import com.citacomsys.module.api.customer.CustomerRepository
import com.citacomsys.sales.ui.screen.share.SaleReceiptHandler
import com.citacomsys.sales.ui.screen.share.SmsReceiptDialogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SalesDetailsUiState(
    val saleDetailsState: UiResult<SaleDetails> = UiResult.Loading,
    val receiptPreview: UiResult<List<ImageBitmap>> = UiResult.Loading,
    val receiptOptState: UiResult<Any> = UiResult.Idle
)

class SaleDetailsViewModel(
    private val saleId: String,
    notificationService: InAppNotificationService,
    val receiptHandler: SaleReceiptHandler,
    sessionManager: AppSessionManager,
    private val customerRepository: CustomerRepository
) : BaseViewModel(notificationService) {

    val actor = sessionManager.appUser
    val activeWorkspaceId: String = sessionManager.activeWorkspaceId ?: ""

    private val _associatedCustomer = MutableStateFlow<UiResult<CustomerItem>>(UiResult.Loading)
    val associatedCustomer: StateFlow<UiResult<CustomerItem>> = _associatedCustomer.asStateFlow()

    val ledgerEntries: StateFlow<List<CustomerLedgerEntry>> = flow {
        val list = runCatching {
            customerRepository.getLedgerForSale(
                saleId,
                activeWorkspaceId
            )
        }.getOrDefault(emptyList())
        emit(list)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Map the handler's internal state to the UI state for this specific screen
    val state: StateFlow<SalesDetailsUiState> = receiptHandler.state
        .map { h ->
            SalesDetailsUiState(
                saleDetailsState = h.details,
                receiptPreview = h.pdfPreview,
                receiptOptState = h.operationState
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SalesDetailsUiState()
        )

    val smsReceiptState = receiptHandler.state.map { it.smsDialog }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SmsReceiptDialogState()
        )

    init {
        receiptHandler.loadDetails(saleId, activeWorkspaceId) {
            generatePdf()
        }
    }

    fun loadDetails() {
        receiptHandler.loadDetails(saleId, activeWorkspaceId)
    }

    fun loadCustomerLedger(customerId: String?, workspaceId: String) {
        if (customerId.isNullOrBlank()) {
            _associatedCustomer.value = UiResult.Idle
            return
        }

        viewModelScope.launch {
            _associatedCustomer.value = UiResult.Loading
            _associatedCustomer.value = customerRepository.findItemById(customerId, workspaceId).asUiResult()
        }
    }

    fun onPhoneChange(phone: String) = receiptHandler.onPhoneChange(phone)

    fun sendDigitalReceipt(onSuccess: () -> Unit) {
        receiptHandler.sendSms(saleId, activeWorkspaceId, onSuccess)
    }

    fun print() {
        val staffName = actor.value?.name ?: "Staff"
        receiptHandler.printReceiptWithPrinterModule(staffName)
    }

    fun savePdf() = receiptHandler.savePdf(saleId)

    fun generatePdf() {
        receiptHandler.generatePdfPreview(actor.value!!.name)
    }
}
