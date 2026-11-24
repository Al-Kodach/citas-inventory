package com.citacomsys.feature.organization.ui.details

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.api.workspace.model.WorkspaceDetails
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkspaceDetailsViewModel(
    private val api: OrganizationService,
    private val store: PlatformRuntimeStore,
    private val sessionManager: AppSessionManager,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    private val _workspaceDetails = MutableStateFlow<UiResult<WorkspaceDetails>>(UiResult.Idle)
    val workspaceDetails = _workspaceDetails.asStateFlow()

    private val _launchState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val launchState = _launchState.asStateFlow()

    fun loadWorkspaceDetails(workspaceId: String) {
        viewModelScope.launch {
            _workspaceDetails.value = UiResult.Loading
            api.getWorkspaceDetails(workspaceId)
                .onSuccess { details ->
                    _workspaceDetails.value = UiResult.Success(details)
                }
                .onFailure { error ->
                    val message = error.message ?: "Failed to load workspace details"
                    val local = getDetails(workspaceId)
                    if (local != null) {
                        _workspaceDetails.value = UiResult.Success(local)
                    } else {
                        _workspaceDetails.value = UiResult.Error(message)
                    }
                    showError(message)
                }
        }
    }

    private fun getDetails(workspaceId: String): WorkspaceDetails? {
        val branches = store.organizationConfig.value?.workspaces?.find { it.id == workspaceId }?.branches
        val config = store.getWorkspaceConfig(workspaceId) ?: return null
        return WorkspaceDetails(
            workspaceId = workspaceId,
            organizationId = "",
            name = config.name,
            status = config.status,
            type = config.type,
            resource = config.resource,
            usage = config.usage,
            branches = branches ?: emptyList()
        )
    }

    fun launchWorkspace(
        workspaceId: String,
        branchId: String?,
        isWsLocked: Boolean,
        isBranchLocked: Boolean
    ) {
        viewModelScope.launch {
            _launchState.value = UiResult.Loading
            val details = (_workspaceDetails.value as? UiResult.Success)?.data ?: run {
                showError("Business info not found")
                _launchState.value = UiResult.Error("Business info not found")
                return@launch
            }
            val targetBranchId = branchId ?: workspaceId

            val authError = sessionManager.isAuthorized(workspaceId, targetBranchId)
            if (authError != null) {
                showError(authError)
                _launchState.value = UiResult.Error(authError)
                return@launch
            }

            if (sessionManager.activeWorkspaceId == workspaceId) {
                EventBus.publish(
                    AppLifecycleEvent.WorkspaceRestore(
                        workspaceId = workspaceId,
                        branchId = targetBranchId,
                        isWsLocked = isWsLocked,
                        isBranchLocked = isBranchLocked,
                    )
                )
                _launchState.value = UiResult.Success(Unit)
                return@launch
            }

            EventBus.publish(
                AppLifecycleEvent.WorkspaceSelected(
                    workspaceId = workspaceId,
                    branchId = targetBranchId,
                    isWsLocked = isWsLocked,
                    isBranchLocked = isBranchLocked,
                    productType = details.type
                )
            )

            _launchState.value = UiResult.Success(Unit)
        }
    }
}
