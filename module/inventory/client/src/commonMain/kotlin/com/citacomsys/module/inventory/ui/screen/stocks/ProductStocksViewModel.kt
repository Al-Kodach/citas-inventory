package com.citacomsys.module.inventory.ui.screen.stocks

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.types.BatchItem
import com.citacomsys.module.inventory.domain.repository.ClientStockBatchRepository
import com.citacomsys.module.inventory.request.AdjustStockRequest
import com.citacomsys.types.StockMovement
import com.citacomsys.types.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StockStatusFilter {
    ALL,
    LOW_STOCK,
    EXPIRING_SOON
}

/**
 * ViewModel for managing the physical inventory (batches and movements) of a specific product.
 * This ViewModel serves the 'Detail' pane in the adaptive inventory layout.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductStocksViewModel(
    notification: InAppNotificationService,
    private val repository: ClientStockBatchRepository,
    private val sessionManager: AppSessionManager,
) : BaseViewModel(notification) {
    val productType = sessionManager.activeProductType

    private val actor: User = sessionManager.appUser.value ?: error("Your session has expired")
    private val _productId = MutableStateFlow<String?>(null)

    // --- Search & Filter States ---
    val searchQuery = MutableStateFlow("")
    val stockStatusFilter = MutableStateFlow(StockStatusFilter.ALL)

    // --- Filters for Movements ---
    private val _batchFilter = MutableStateFlow<String?>(null)
    val batchFilter = _batchFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow<String?>(null) // "IN" or "OUT"
    val typeFilter = _typeFilter.asStateFlow()

    /**
     * Reactively observes, searches, and filters batches for the currently selected product context.
     */
    val batches: StateFlow<UiResult<List<BatchItem>>> = combine(
        _productId.filterNotNull().flatMapLatest { repository.observeBatchesForProduct(it) },
        searchQuery,
        stockStatusFilter
    ) { list, query, filter ->
        val filtered = list.filter { batch ->
            val matchesSearch =
                query.isBlank() || batch.batchNumber.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                StockStatusFilter.ALL -> true
                StockStatusFilter.LOW_STOCK -> batch.availableQuantity <= 0
                StockStatusFilter.EXPIRING_SOON -> true
            }
            matchesSearch && matchesFilter
        }
        UiResult.Success(filtered)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiResult.Loading
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onStatusFilterChange(filter: StockStatusFilter) {
        stockStatusFilter.value = filter
    }

    /**
     * Reactively observes and filters movements for the currently selected product context.
     */
    val movements: StateFlow<UiResult<List<StockMovement>>> =
        combine(
            _productId.filterNotNull()
                .flatMapLatest { repository.observeStockMoventByProduct(it) },
            _batchFilter,
            _typeFilter
        ) { list, batchId, type ->
            val filtered = list.filter { m ->
                (batchId == null || m.batchId == batchId) &&
                        (type == null || m.type.name == type)
            }
            UiResult.Success(filtered)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiResult.Loading
        )


    fun loadStocks(productId: String) {
        _productId.value = productId
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
                showSuccess("Inventory Updated successful")
            }.onFailure { showError(it.message) }
        }
    }

}