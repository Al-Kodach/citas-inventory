package com.citacomsys.feature.organization.ui.hub

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.kernel.model.OrganizationRuntimeConfig
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.shared.util.mapStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrganizationHubViewModel(
    private val platformService: PlatformClientService,
    private val platformStore: PlatformRuntimeStore,
    private val meshManager: MeshManager,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    val config = platformStore.organizationConfig
        .asUiResult().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UiResult.Loading
        )

    private val _refreshState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val refreshState = _refreshState.asStateFlow()

    private val _nodeActivities = MutableStateFlow<List<NodeActivity>>(emptyList())
    val nodeActivities = _nodeActivities.asStateFlow()

    init {
        viewModelScope.launch {
            meshManager.meshSignals.collect { activity ->
                _nodeActivities.update { current ->
                    listOf(activity) + current.take(49)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = UiResult.Loading
            platformService.refreshOrganizationConfig()
                .onSuccess {
                    _refreshState.value = UiResult.Success(Unit)
                    showSuccess("Organization Hub refreshed successfully")
                }
                .onFailure {
                    val message = it.message ?: "Failed to refresh organization data"
                    _refreshState.value = UiResult.Error(message)
                    showError("Refresh failed: $message")
                }
        }
    }
}
