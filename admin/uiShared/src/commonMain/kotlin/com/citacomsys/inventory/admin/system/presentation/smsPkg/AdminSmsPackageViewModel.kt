package com.citacomsys.inventory.admin.system.presentation.smsPkg

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.system.SystemApiService
import com.citacomsys.api.platform.model.SmsPackage
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AdminSmsPackageViewModel(
    private val api: SystemApiService,
    notificationService: InAppNotificationService,
    sessionManager: AppSessionManager
) : SaaSListViewModel<SmsPackage>(notificationService, sessionManager) {

    override val entityType = EntityType.PACKAGE

    override val itemsState: StateFlow<UiResult<List<SmsPackage>>> = pageQueryFlow
        .flatMapLatest { query ->
            flow {
                emit(UiResult.Loading)
                runCatching {
                    val all = api.fetchSmsPackages()
                    val searchQuery = query.search
                    if (searchQuery.isNullOrBlank()) all
                    else all.filter { it.units.toString().contains(searchQuery) }
                }.onSuccess { filtered ->
                    updateLoadState(canLoadMore = false, isLoadingMore = false)
                    emit(UiResult.Success(filtered))
                }.onFailure { err ->
                    updateLoadState(isLoadingMore = false, error = err.message)
                    emit(UiResult.Error(err.message ?: "Failed to fetch SMS packages"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )

    fun savePackage(pkg: SmsPackage) {
        viewModelScope.launch {
            api.saveSmsPackage(pkg).onSuccess {
                showSuccess("SMS package saved")
                refresh()
            }.onFailure {
                showError(it.message ?: "Failed to save SMS package")
            }
        }
    }
}
