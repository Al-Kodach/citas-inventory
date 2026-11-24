package com.citacomsys.inventory.admin.system.presentation.pkg

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.system.SystemApiService
import com.citacomsys.api.platform.model.ProductPackage
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

/**
 * ViewModel for managing the list of product packages in the platform.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminPackageListViewModel(
    notificationService: InAppNotificationService,
    sessionManager: AppSessionManager,
    private val api: SystemApiService
) : SaaSListViewModel<ProductPackage>(notificationService, sessionManager) {

    override val entityType: EntityType = EntityType.PACKAGE

    override val itemsState: StateFlow<UiResult<List<ProductPackage>>> = pageQueryFlow
        .flatMapLatest { query ->
            flow {
                emit(UiResult.Loading)
                runCatching { api.fetchPackages(query) }
                    .onSuccess { pageResult ->
                        updateLoadState(canLoadMore = !pageResult.isLast, isLoadingMore = false)
                        emit(UiResult.Success(pageResult.items))
                    }
                    .onFailure { err ->
                        updateLoadState(isLoadingMore = false, error = err.message)
                        emit(UiResult.Error(err.message ?: "Failed to fetch packages"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )
}
