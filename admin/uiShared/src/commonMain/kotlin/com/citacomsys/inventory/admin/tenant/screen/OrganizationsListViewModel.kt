package com.citacomsys.inventory.admin.tenant.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.tenant.TenantApiService
import com.citacomsys.api.organization.model.Organization
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.management.SaaSListViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.types.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationsListViewModel(
    private val api: TenantApiService,
    sessionManager: AppSessionManager,
    notificationService: InAppNotificationService
) : SaaSListViewModel<Organization>(notificationService, sessionManager) {

    override val entityType: EntityType = EntityType.TENANT

    override val itemsState: StateFlow<UiResult<List<Organization>>> = pageQueryFlow
        .flatMapLatest { query ->
            flow {
                emit(UiResult.Loading)
                runCatching { api.fetchOrganizations(query) }
                    .onSuccess { pageResult ->
                        updateLoadState(canLoadMore = !pageResult.isLast, isLoadingMore = false)
                        emit(UiResult.Success(pageResult.items))
                    }
                    .onFailure { err ->
                        updateLoadState(isLoadingMore = false, error = err.message)
                        emit(UiResult.Error(err.message ?: "Failed to fetch organizations"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )
}
