package com.citacomsys.module.inventory.ui.screen.stocks

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.workspace.branch.BranchRepository
import com.citacomsys.core.domain.security.BranchUiModel
import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.BatchItem
import com.citacomsys.module.inventory.data.remote.api.StockApiService
import com.citacomsys.module.inventory.domain.repository.ClientStockBatchRepository
import com.citacomsys.module.inventory.model.StockTransferPayload
import com.citacomsys.types.EntityType
import com.citacomsys.types.StockBatch
import com.citacomsys.types.generateUUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val batch: StockBatch? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class StockTransferViewModel(
    private val batchId: String,
    notification: InAppNotificationService,
    private val sessionManager: AppSessionManager,
    private val apiService: StockApiService,
    private val syncEngine: EntitySyncEngine,
    private val connectivityService: NetworkConnectivityService,
    private val stockBatchRepository: ClientStockBatchRepository,
    private val store: PlatformRuntimeStore,
) : BaseViewModel(notification) {


    val availableBranches =
        store.organizationConfig
            .map {
                it!!.workspaces.first { it.id == sessionManager.activeWorkspaceId }
                    .branches
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    private val _state = MutableStateFlow(UiState())

    val selectedBatch =
        combine(
            stockBatchRepository.observeBatch(batchId),
            _state
        ) { local, state ->
            if (local != null) {
                UiResult.Success(local)
            } else {
                if (state.isLoading) UiResult.Loading
                else if (state.error != null) UiResult.Error(state.error)
                else {
                    if (state.batch == null && sessionManager.isSubscribed()) refresh()
                    UiResult.Success(null)
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiResult.Loading
        )

    val destinationBranchId = MutableStateFlow("")
    val quantityToTransfer = MutableStateFlow("")
    val reasonMemo = MutableStateFlow("")

    private val _actionState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val actionState = _actionState.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting = _isExecuting.asStateFlow()


    fun refresh() {
        viewModelScope.launch {
            if (sessionManager.isSubscribed()) {
                _state.value = UiState(isLoading = true, error = null)
                stockBatchRepository.fetchBatchDetailsCloud(batchId)
                    .onSuccess {
                        _state.value = UiState(isLoading = false, batch = it)

                    }.onFailure {
                        _state.value = UiState(isLoading = false, error = it.message ?: "Failed to get batch")
                    }
            }
        }
    }


    /**
     * Executes the online-only transfer request.
     */
    fun submitTransferRequest(onSuccess: () -> Unit) {
        val batch = selectedBatch.value.getOrNull() ?: return
        val qty = quantityToTransfer.value.toIntOrNull() ?: 0
        val memo = reasonMemo.value.trim()
        val destination = destinationBranchId.value


        // --- FORM ACTION VALIDATION SAFETY GATES ---
        if (destination.isBlank()) {
            showError("Please pick a destination branch.")
            return
        }
        if (qty <= 0) {
            showError("Transfer quantity must be greater than zero.")
            return
        }
        if (qty > batch.availableQuantity) {
            showError("Insufficient stock inside origin container. Max available: ${batch.availableQuantity}")
            return
        }
        if (memo.isBlank()) {
            showError("A reason or audit memo note is required for tracking transfers.")
            return
        }

        viewModelScope.launch {
            if (!connectivityService.isOnline()) {
                showError("Inter-branch stock transfers are online-only operations to protect ledger history.")
                return@launch
            }

            _isExecuting.value = true
            _actionState.value = UiResult.Loading

            val payload = StockTransferPayload(
                id = generateUUID(),
                workspaceId = sessionManager.activeWorkspaceId!!,
                productId = batch.productId,
                batchId = batch.batchId,
                sourceBranchId = batch.branchId,
                destinationBranchId = destination,
                quantity = qty,
                reason = memo,
                actorId = sessionManager.appUser.value?.id ?: "SYSTEM"
            )

            val networkResult = runCatching {
                apiService.submitTransfer(payload)
            }

            networkResult.onSuccess {
                showSuccess("Transfer processed successfully on the cloud ledger!")

                // ⚡ Force the local sync engine to pull down the newly emitted IN / OUT movement lines
                syncEngine.requestSync(EntityType.STOCK_BATCH)

                _isExecuting.value = false
                _actionState.value = UiResult.Idle
                onSuccess()
            }.onFailure { error ->
                showError(error.message ?: "Server rejected your stock transfer sequence.")
                _isExecuting.value = false
                _actionState.value = UiResult.Error(error.message ?: "Unknown Server Error")
            }
        }
    }
}
