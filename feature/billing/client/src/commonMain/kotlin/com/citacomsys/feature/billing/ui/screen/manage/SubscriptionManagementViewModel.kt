package com.citacomsys.feature.billing.ui.screen.manage

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.billing.service.BillingApiService
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.remote.ProductPackagePresentationService
import com.citacomsys.api.platform.uiModel.BillTermUiState
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.api.workspace.model.SubscriptionSummary
import com.citacomsys.api.workspace.service.WorkspaceApiService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubscriptionManagementState(
    val currentSubscription: UiResult<SubscriptionSummary> = UiResult.Idle,
    val availablePackages: UiResult<List<ProductPackageUiState>> = UiResult.Idle,
    val selectedPackage: ProductPackageUiState? = null,
    val selectedTerm: BillTermUiState? = null,
    val isLoading: Boolean = false,
    val previewState: UiResult<InvoicePreviewUiState> = UiResult.Idle,
    val errorMessage: String? = null
)

class SubscriptionManagementViewModel(
    private val tenantApi: WorkspaceApiService,
    private val packageService: ProductPackagePresentationService,
    private val billingApiService: BillingApiService,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    private val _state = MutableStateFlow(SubscriptionManagementState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Parallel fetch of current status and available upgrades
            val sub = runCatching { tenantApi.getSubscriptionSummary() }.asUiResult()
            val pkgs = runCatching { packageService.getPublicPackageUiStates() }.asUiResult()

            _state.update {
                it.copy(
                    currentSubscription = sub,
                    availablePackages = pkgs,
                    isLoading = false
                )
            }
        }
    }

    fun selectPackage(pkg: ProductPackageUiState) {
        _state.update {
            it.copy(
                selectedPackage = pkg,
                selectedTerm = pkg.billingTerms.firstOrNull()
            )
        }
    }

    fun selectTerm(term: BillTermUiState) {
        _state.update { it.copy(selectedTerm = term) }
    }

    fun loadInvoicePreview() {
        viewModelScope.launch {
            _state.update { it.copy(previewState = UiResult.Loading) }

            val request = PreviewRequest.Subscription(
                packageId = state.value.selectedPackage!!.id,
                billTermId = state.value.selectedTerm!!.id
            )

            val result = runCatching {
                billingApiService.getInvoicePreview(request)
            }.asUiResult()
            _state.update { it.copy(previewState = result) }
        }
    }


    fun confirmUpgrade() {


        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val request = PkgProvisionRequest(
                packageId = state.value.selectedPackage!!.id,
                billTermId = state.value.selectedTerm!!.id
            )

            val result = runCatching { billingApiService.upgradeSubscription(request) }
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        selectedPackage = null,
                        selectedTerm = null
                    )
                }
                showSuccess("Upgrade Successful")
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
                showError("Upgrade Failed")
            }
        }

    }
}