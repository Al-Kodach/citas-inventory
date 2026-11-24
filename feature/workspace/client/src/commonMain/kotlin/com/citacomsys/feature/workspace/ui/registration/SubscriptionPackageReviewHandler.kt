package com.citacomsys.feature.workspace.ui.registration

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.remote.ProductPackagePresentationService
import com.citacomsys.api.platform.uiModel.BillTermUiState
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.types.ResourcePriceBreakdown
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class PackageReviewState(
    val selectedPackage: ProductPackageUiState? = null,
    val selectedBillingTerm: BillTermUiState? = null,
    val resourcePriceBreakdown: UiResult<ResourcePriceBreakdown> = UiResult.Idle,
) {
    val pkgProvisionRequest
        get() = PkgProvisionRequest(
            packageId = selectedPackage?.id ?: "",
            billTermId = selectedBillingTerm?.id ?: ""
        )

    val note = selectedPackage?.note
}

class SubscriptionPackageReviewHandler(
    private val scope: CoroutineScope,
    private val presentationService: ProductPackagePresentationService,
    kSafe: KSafe,
    private val onResult: (PackageReviewState) -> Unit
) {
    private val _packagesFlow =
        MutableStateFlow<UiResult<List<ProductPackageUiState>>>(UiResult.Idle)
    val packages = _packagesFlow.asStateFlow()

    private val _state by kSafe.asMutableStateFlow(PackageReviewState(), scope)
    val state = _state.asStateFlow()

    init {
        scope.launch {
            state.collect {
                onResult(it)
                if (it.selectedPackage != null && it.selectedBillingTerm != null) {
                    calculateRemoteQuote()
                }
            }
        }
    }

    private suspend fun calculateRemoteQuote() {
        _state.update { it.copy(resourcePriceBreakdown = UiResult.Loading) }
        val result =
            runCatching { presentationService.calculateQuote(_state.value.pkgProvisionRequest) }.asUiResult()
        _state.update { it.copy(resourcePriceBreakdown = result) }
    }

    fun loadBreakdown() {
        scope.launch {
            calculateRemoteQuote()
        }
    }

    fun selectPackage(pkg: ProductPackageUiState) {
        val defaultBillTerm = pkg.billingTerms.firstOrNull { it.isDefault }
            ?: pkg.billingTerms.firstOrNull()

        _state.update {
            it.copy(
                selectedPackage = pkg,
                selectedBillingTerm = defaultBillTerm
            )
        }
    }

    fun selectTerm(billingTerm: BillTermUiState) {
        _state.update {
            it.copy(selectedBillingTerm = billingTerm)
        }
    }

    fun loadPkgs() {
        if (_packagesFlow.value.isLoading || _packagesFlow.value.isSuccess) return

        scope.launch {
            _packagesFlow.value = UiResult.Loading
            val result = runCatching { presentationService.getPublicPackageUiStates() }.asUiResult()
            _packagesFlow.value = result
        }
    }

    fun clearState() {
        _state.value = PackageReviewState()
        _packagesFlow.value = UiResult.Idle
    }

    fun refreshState() {
        scope.launch {
            val cachedPkg = _state.value.selectedPackage ?: return@launch
            val cachedTerm = _state.value.selectedBillingTerm ?: return@launch

            // Wait for packages to load
            val packagesResult =
                _packagesFlow.filter { it is UiResult.Success }.first() as UiResult.Success

            val pkg = packagesResult.data.find { it.id == cachedPkg.id }
                ?: run {
                    clearState()
                    return@launch
                }

            val term = pkg.billingTerms.find { it.id == cachedTerm.id }

            _state.update {
                it.copy(
                    selectedPackage = pkg,
                    selectedBillingTerm = term,
                    resourcePriceBreakdown = UiResult.Idle
                )
            }
        }
    }
}
