package com.citacomsys.sales.ui.screen.sales

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.management.SaaSListViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.database.SaleTransactionView
import com.citacomsys.sales.data.repository.ClientSalesRepository
import com.citacomsys.types.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class SalesHistoryViewModel(
    notificationService: InAppNotificationService,
    private val saleRepository: ClientSalesRepository,
    sessionManager: AppSessionManager,
) : SaaSListViewModel<SaleTransactionView>(notificationService, sessionManager) {

    override val entityType = EntityType.SALE

    override val itemsState: StateFlow<UiResult<List<SaleTransactionView>>> = pageQueryFlow
        .flatMapLatest { query ->
            val searchQuery = query.search
            if (searchQuery.isNullOrBlank()) {
                saleRepository.observeSales()
            } else {
                saleRepository.searchSales(searchQuery)
            }.map { list -> UiResult.Success(list) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )
}
