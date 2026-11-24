package com.citacomsys.client.customer.ui.screen.list

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.management.SaaSListViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.module.api.customer.CustomerRepository
import com.citacomsys.types.EntityType
import com.citacomsys.types.UserCapability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListViewModel(
    private val customerRepo: CustomerRepository,
    sessionManager: AppSessionManager,
    notificationService: InAppNotificationService,
) : SaaSListViewModel<CustomerItem>(notificationService, sessionManager) {

    override val entityType = EntityType.CUSTOMER


    override val itemsState: StateFlow<UiResult<List<CustomerItem>>> = pageQueryFlow
        .flatMapLatest { query ->
            flow {
                emit(UiResult.Loading)
                runCatching {
                    customerRepo.fetchPaged(
                        "",
                        query.copy(workspaceId = sessionManager.activeWorkspaceId)
                    )
                }.onSuccess { paged ->
                    emit(UiResult.Success(paged.items))
                }.onFailure { e ->
                    emit(UiResult.Error(e.message ?: "Failed to load customers"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )
}
