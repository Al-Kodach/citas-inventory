package com.citacomsys.module.inventory.ui.screen.product

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.management.SaaSListViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.database.ProductListItemView
import com.citacomsys.module.inventory.data.repository.BaseClientProductRepository
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModel(
    notification: InAppNotificationService,
    private val productRepo: BaseClientProductRepository<out BaseProduct>,
) : SaaSListViewModel<ProductListItemView>(notification, productRepo.sessionManager) {

    override val entityType = EntityType.PRODUCT

    override val itemsState: StateFlow<UiResult<List<ProductListItemView>>> = pageQueryFlow
        .flatMapLatest { query ->
            productRepo.observeProducts(query)
                .map { list -> UiResult.Success(list) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )

    private val _expandedProductId = MutableStateFlow<String?>(null)
    val expandedProductId = _expandedProductId.asStateFlow()

    private val _isImporting = MutableStateFlow<String?>(null)
    val isImporting = _isImporting.asStateFlow()

    fun toggleExpand(productId: String) {
        _expandedProductId.value = if (_expandedProductId.value == productId) null else productId
    }
}
