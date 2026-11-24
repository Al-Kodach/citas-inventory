package com.citacomsys.feature.billing.ui.screen.sms

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.billing.service.BillingApiService
import com.citacomsys.api.platform.remote.ProductPackagePresentationService
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest
import com.citacomsys.api.platform.uiModel.SmsPackageUiState
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmsPurchaseState(
    val packagesResult: UiResult<List<SmsPackageUiState>> = UiResult.Idle,
    val selectedPackageId: String? = null,
    val isLoading: Boolean = false,
    val previewState: UiResult<InvoicePreviewUiState> = UiResult.Idle,
)

class SmsPurchaseViewModel(
    private val packagePresentationService: ProductPackagePresentationService,
    private val billingApiService: BillingApiService,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    private val _state = MutableStateFlow(SmsPurchaseState())
    val state = _state.asStateFlow()

    init {
        loadPackages()
    }

    /**
     * Fetch available SMS bundles from the package service
     */
    fun loadPackages() {
        viewModelScope.launch {
            _state.update { it.copy(packagesResult = UiResult.Loading) }
            val result = runCatching {
                packagePresentationService.getSmsPackages()
            }.asUiResult()

            _state.update { it.copy(packagesResult = result) }
        }
    }

    /**
     * Updates the local selection state
     */
    fun selectPackage(packageId: String) {
        _state.update { it.copy(selectedPackageId = packageId) }
    }

    fun loadInvoicePreview(packageId: String) {
        viewModelScope.launch {
            _state.update { it.copy(previewState = UiResult.Loading) }
            val result = runCatching {
                packagePresentationService.getInvoicePreview(PreviewRequest.SmsTopup(packageId))
            }.asUiResult()
            _state.update { it.copy(previewState = result) }
        }
    }

    /**
     * Converts the selected package into an actual Billing Record (Invoice)
     * This moves the flow from Selection/Preview into the Manual Payment verification flow.
     */
    fun confirmSmsPurchase(packageId: String, onSuccess: (billId: String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            runCatching {
                billingApiService.createSmsTopupInvoice(packageId)
            }.onSuccess { bill ->
                _state.update { it.copy(isLoading = false) }
                showSuccess("Invoice Generated Successfully")
                onSuccess(bill.id)
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false) }
                showError(e.message ?: "Failed to generate SMS invoice")
            }
        }
    }
}

