package com.citacomsys.module.inventory.ui.screen.batch

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.types.StockBatch
import com.citacomsys.module.inventory.domain.repository.ClientStockBatchRepository
import com.citacomsys.module.inventory.request.AdjustStockRequest
import com.citacomsys.types.StockMovement
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel dedicated to managing single batch details, operations, and movements.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BatchDetailsViewModel(
    notification: InAppNotificationService,
    private val repository: ClientStockBatchRepository,
    private val sessionManager: AppSessionManager,
    private val capsManager: UserCapabilityManager
) : BaseViewModel(notification) {

    private val actor: User = sessionManager.appUser.value ?: error("Your session has expired")
    private val _batchId = MutableStateFlow<String?>(null)
    private val _productId = MutableStateFlow<String?>(null)

    val canViewCostPrice get() = capsManager.has(UserCapability.CORRECT_STOCK_COSTS)
    val canAdjustStock get() = capsManager.has(UserCapability.MANAGE_INVENTORY)
    val canEditPrice get() = capsManager.has(UserCapability.MANAGE_INVENTORY_PRICES)

    private val _isSyncingCloudMovements = MutableStateFlow(false)
    val isSyncingCloudMovements: StateFlow<Boolean> = _isSyncingCloudMovements.asStateFlow()

    fun load(productId: String, batchId: String) {
        _productId.value = productId
        _batchId.value = batchId
        viewModelScope.launch {
            val localBatch = repository.findById(batchId)
            if (localBatch == null && sessionManager.isSubscribed()) {
                repository.fetchBatchDetailsCloud(batchId)
            }
        }
    }

    /**
     * Reactively observes the batch metadata.
     */
    val batchDetails: StateFlow<UiResult<StockBatch>> = _batchId
        .filterNotNull()
        .flatMapLatest { id ->
            repository.observeBatchView(id)
                .map { view ->
                    if (view != null) UiResult.Success(view)
                    else UiResult.Loading
                }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiResult.Loading
        )

    /**
     * Reactively observes local stock movements specifically for this batch.
     */
    val movements: StateFlow<UiResult<List<StockMovement>>> = _batchId
        .filterNotNull()
        .flatMapLatest { id ->
            repository.observeMovementsForBatch(id)
                .map { list -> UiResult.Success(list) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiResult.Loading
        )

    fun syncCloudMovements() {
        val productId = _productId.value ?: return
        val batchId = _batchId.value ?: return
        viewModelScope.launch {
            _isSyncingCloudMovements.value = true
            repository.syncMovementsFromCloud(productId, batchId)
                .onSuccess {
                    showSuccess("Movements synced from cloud")
                }
                .onFailure {
                    showError(it.message ?: "Failed to sync cloud movements")
                }
            _isSyncingCloudMovements.value = false
        }
    }

    fun adjust(batchId: String, quantity: Int, reason: String) {
        val productId = _productId.value ?: return
        viewModelScope.launch {
            val request = AdjustStockRequest(
                batchId = batchId,
                workspaceId = sessionManager.activeWorkspaceId!!,
                branchId = sessionManager.resolveWriteBranchId(),
                productId = productId,
                quantity = quantity,
                reason = reason
            )
            repository.adjustStock(
                request,
                actorId = sessionManager.currentUserId!!,
                actorName = sessionManager.actorName!!
            ).onSuccess {
                showSuccess("Inventory updated successfully")
            }.onFailure { showError(it.message) }
        }
    }

    fun deleteBatch(batchId: String) {
        viewModelScope.launch {
            runCatching {
                repository.delete(
                    batchId,
                    actorId = sessionManager.currentUserId!!,
                    actorName = sessionManager.actorName!!
                )
            }
                .onSuccess { showSuccess("Batch removed") }
                .onFailure { showError(it.message) }
        }
    }
}
