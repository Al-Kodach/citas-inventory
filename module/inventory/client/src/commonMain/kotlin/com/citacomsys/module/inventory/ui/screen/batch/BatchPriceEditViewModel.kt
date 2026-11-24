package com.citacomsys.module.inventory.ui.screen.batch

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.module.inventory.domain.repository.ClientStockBatchRepository
import com.citacomsys.module.inventory.request.PriceChangeRequest
import com.citacomsys.module.inventory.request.PriceType
import com.citacomsys.types.Money
import com.citacomsys.types.StockBatch
import com.citacomsys.types.UserCapability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * ViewModel for managing the price update form (Cost or Selling Price).
 * Decouples the UI from the persistence and validation logic.
 */
class BatchPriceEditViewModel(
    notification: InAppNotificationService,
    private val repository: ClientStockBatchRepository,
    private val sessionManager: AppSessionManager,
    private val capabilityManager: UserCapabilityManager
) : BaseViewModel(notification) {

    private val _batchState = MutableStateFlow<UiResult<StockBatch>>(UiResult.Idle)
    val batchState = _batchState.asStateFlow()

    private val _saveState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val saveState = _saveState.asStateFlow()

    // --- Form State ---
    val newPriceStr = MutableStateFlow("")
    val reason = MutableStateFlow("")

    /**
     * Loads the specific batch and initializes the price based on the update type.
     */
    fun loadBatch(batchId: String, priceType: PriceType) {
        viewModelScope.launch {
            _batchState.value = UiResult.Loading
            val batch = repository.findById(batchId)
            if (batch != null) {
                _batchState.value = UiResult.Success(batch)
                // Initialize with current value
                val currentPrice = if (priceType == PriceType.COST) batch.costPrice else batch.sellingPrice
                newPriceStr.value = currentPrice.amount.toString()
                reason.value = ""
            } else {
                _batchState.value = UiResult.Error("Target batch not found")
            }
        }
    }

    /**
     * Persists the price change to the ledger.
     */
    fun savePriceUpdate(priceType: PriceType, onSuccess: () -> Unit) {
        val batch = _batchState.value.getOrNull() ?: return
        val price = newPriceStr.value.toDoubleOrNull()

        if (!capabilityManager.has(UserCapability.MANAGE_INVENTORY_PRICES)) {
            showError("You don't have permission to change basePrice")
            return
        }

        if (price == null) {
            showError("Please enter a valid numeric amount")
            return
        }

        if (priceType == PriceType.COST && reason.value.isBlank()) {
            showError("A reason is mandatory for cost price adjustments")
            return
        }

        if (!capabilityManager.has(UserCapability.MANAGE_INVENTORY_PRICES)) {
            showError("You don't have permission to change basePrice")
            return
        }

        viewModelScope.launch {
            _saveState.value = UiResult.Loading

            val request = PriceChangeRequest(
                batchId = batch.id,
                workspaceId = batch.workspaceId,
                branchId = batch.branchId,
                newPrice = Money.fromMajor(price, batch.costPrice.currencyCode),
                type = priceType,
                reason = reason.value.takeIf { it.isNotBlank() },
                createdAt = Clock.System.now()
            )

            val result = repository.updatePrice(
                request,
                actorId = sessionManager.currentUserId!!,
                actorName = sessionManager.actorName!!
            )

            result.onSuccess {
                showSuccess("Inventory valuation updated")
                onSuccess()
                _saveState.value = UiResult.Idle
            }.onFailure {
                showError(it.message ?: "Failed to update price")
                _saveState.value = UiResult.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun clearState() {
        _batchState.value = UiResult.Idle
        _saveState.value = UiResult.Idle
        newPriceStr.value = ""
        reason.value = ""
    }
}