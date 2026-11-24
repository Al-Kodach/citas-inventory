package com.citacomsys.inventory.admin.device.presentation

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.device.AdminDeviceApiService
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.management.SaaSListViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for managing the administrative device list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceListViewModel(
    notificationService: InAppNotificationService,
    sessionManager: AppSessionManager,
    private val api: AdminDeviceApiService
) : SaaSListViewModel<DeviceSummary>(notificationService, sessionManager) {

    override val entityType: EntityType = EntityType.DEVICE

    override val itemsState: StateFlow<UiResult<List<DeviceSummary>>> = pageQueryFlow
        .flatMapLatest { query ->
            flow {
                emit(UiResult.Loading)
                runCatching { api.fetch(query) }
                    .onSuccess { pageResult ->
                        updateLoadState(canLoadMore = !pageResult.isLast, isLoadingMore = false)
                        emit(UiResult.Success(pageResult.items))
                    }
                    .onFailure { err ->
                        updateLoadState(isLoadingMore = false, error = err.message)
                        emit(UiResult.Error(err.message ?: "Failed to fetch device list"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )
}
