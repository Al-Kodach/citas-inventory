package com.citacomsys.feature.organization.ui.workspace

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.AlertService
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getOrEmpty
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.ProductType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkspaceListViewModel(
    private val store: PlatformRuntimeStore,
    private val sessionManager: AppSessionManager
) : ViewModel() {
    private val _workspacesState = MutableStateFlow<UiResult<List<WorkspaceSummary>>>(UiResult.Loading)
    val workspacesState = _workspacesState.asStateFlow()

    val activeWorkspaceId = sessionManager.activeWorkspaceIdFlow

    var searchQuery = MutableStateFlow("")
    var selectedTypeFilter = MutableStateFlow<ProductType?>(null)

    val filteredWorkspaces = combine(
        _workspacesState.debounce(500),
        searchQuery,
        selectedTypeFilter
    ) { wsState, query, filter ->
        wsState.getOrEmpty().filter { ws ->
            val matchesSearch = query.isBlank() ||
                    ws.name.contains(query, ignoreCase = true) ||
                    ws.type.name.contains(query, ignoreCase = true)
            val matchesType = filter == null || ws.type == filter
            matchesSearch && matchesType
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        Napier.d { "Loading workspaces" }
        viewModelScope.launch {
            _workspacesState.value = UiResult.Loading
            _workspacesState.value = runCatching { store.getWorkspaces() }.asUiResult()
        }
    }

    fun launchWs(
        isWsLocked: Boolean,
        isBranchLocked: Boolean,
        workspace: WorkspaceSummary,
        branchId: String
    ) {
        viewModelScope.launch {
            val authError = sessionManager.isAuthorized(workspace.id, branchId)
            if (authError != null) {
                AlertService.showError(authError)
                return@launch
            }

            sessionManager.setWorkspaceLocked(isWsLocked, workspace.id)

            EventBus.publish(
                AppLifecycleEvent.WorkspaceSelected(
                    workspaceId = workspace.id,
                    branchId = branchId,
                    isWsLocked = isWsLocked,
                    isBranchLocked = isBranchLocked,
                    productType = workspace.type
                )
            )
        }
    }

}