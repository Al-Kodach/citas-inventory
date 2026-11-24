package com.citacomsys.user.ui.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.management.SaaSListViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.UserWorkspaceBindingEvent
import com.citacomsys.shared.event.WorkspaceEvent
import com.citacomsys.types.EntityType
import com.citacomsys.types.UserItem
import com.citacomsys.user.data.ClientUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TenantUserListViewModel(
    notificationService: InAppNotificationService,
    private val userRepository: ClientUserRepository,
    sessionManager: AppSessionManager,
) : SaaSListViewModel<UserItem>(notificationService, sessionManager) {

    override val entityType = EntityType.USER
    val activeProductType = sessionManager.activeProductType

    override val itemsState: StateFlow<UiResult<List<UserItem>>> = pageQueryFlow
        .flatMapLatest { query ->
            flow {
                emit(UiResult.Loading)
                val result = runCatching { userRepository.syncCloudPage(query) }
                result.onSuccess { pageResult ->
                    updateLoadState(canLoadMore = !pageResult.isLast, isLoadingMore = false)
                    emit(UiResult.Success(pageResult.items))
                }.onFailure { err ->
                    updateLoadState(isLoadingMore = false, error = err.message)
                    emit(UiResult.Error(err.message ?: "Failed to fetch user list"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )

    init {
        viewModelScope.launch {
            EventBus.subscribe<UserWorkspaceBindingEvent>().collect {
                refresh()
            }
        }
        viewModelScope.launch {
            EventBus.subscribe<WorkspaceEvent>().collect {
                refresh()
            }
        }
    }
}
