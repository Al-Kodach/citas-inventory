package com.citacomsys.module.inventory.ui.screen.product

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.security.sudo.ActorSudoAuthHandler
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.module.inventory.data.repository.BaseClientProductRepository
import com.citacomsys.shared.util.mapStateFlow
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.UserCapability
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.biometrics.KSafeBiometrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the detailed metadata view and destructive operations for a product.
 */
class ProductMetadataViewModel(
    notification: InAppNotificationService,
    private val productRepo: BaseClientProductRepository<out BaseProduct>,
    actManager: ActionConfirmationManager,
    private val sessionManager: AppSessionManager,
    private val capabilityManager: UserCapabilityManager,
    ksafe: KSafe
) : BaseViewModel(notification) {

    val sudoHandler = ActorSudoAuthHandler(
        actionConfirmationManager = actManager,
        sessionManager = sessionManager,
        scope = viewModelScope,
        prefs = ksafe
    )

    val user = sessionManager.appUser.value!!

    private val selectedProductId = MutableStateFlow<String?>(null)

    private val _productState = MutableStateFlow<UiResult<BaseProduct>>(UiResult.Idle)

    val productLiveView = combine(
        selectedProductId,
        _productState
    ) { view, state ->
        productRepo.observeViewById(selectedProductId.value ?: "").first()
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val productState =
        combine(
            productRepo.observeProduct(selectedProductId.value ?: ""),
            _productState
        ) { cache, state ->
            cache?.let {
                UiResult.Success(it)
            } ?: state
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )

    private val _actionResult = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val actionResult = _actionResult.asStateFlow()

    init {
        viewModelScope.launch {
            selectedProductId.collect { productId ->
                if (productId != null) {
                    loadProductMetadata(productId)
                }
            }
        }
    }

    /**
     * Called when the screen opens or a new product ID is selected.
     */
    fun selected(productId: String?) {
        selectedProductId.value = productId
    }

    /**
     * Reads full product domain model from local SQLite DB.
     */
    private fun loadProductMetadata(productId: String) {
        viewModelScope.launch {
            _productState.value = UiResult.Loading
            _productState.value = productRepo.getById(productId).asUiResult()
        }
    }

    /**
     * Triggered when the user taps "Delete Product".
     * Validates user capability, requests biometric authorization, and executes deletion.
     */
    fun delete() {
        val product = _productState.value.getOrNull() ?: return
        if (!capabilityManager.has(UserCapability.MANAGE_INVENTORY)) {
            showError("Unauthorized: You do not have permission to delete catalog items.")
            return
        }

        viewModelScope.launch {
            if (KSafeBiometrics.biometricsAvailable()) {
                KSafeBiometrics.verifyBiometricDirect(
                    reason = "Delete Product: ${product.name}"
                ) { authenticated ->
                    if (authenticated) {
                        executeDeletion(product.id)
                    } else {
                        showError("Authentication failed")
                    }
                }
            } else {
                executeDeletion(product.id)
            }
        }
    }

    private fun executeDeletion(productId: String) {
        viewModelScope.launch {
            _actionResult.value = UiResult.Loading
            val result = productRepo.delete(
                productId = productId,
                actorId = user.id,
                workspaceId = sessionManager.activeWorkspaceId!!,
                auditBranchId = sessionManager.resolveWriteBranchId()
            )
            _actionResult.value = result.asUiResult()
            result.onSuccess {
                showSuccess("Product deleted successfully")
            }.onFailure {
                showError(it.message ?: "Failed to delete product")
            }
        }
    }
}
