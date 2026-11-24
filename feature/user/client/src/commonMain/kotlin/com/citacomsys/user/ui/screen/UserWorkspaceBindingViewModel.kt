package com.citacomsys.user.ui.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.inventory.api.user.model.BindUserToWorkspaceRequest
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.DefaultCapabilities
import com.citacomsys.types.PageQuery
import com.citacomsys.types.ResourceType
import com.citacomsys.types.TenantUserRole
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserItem
import com.citacomsys.user.data.ClientUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserWorkspaceBindingViewModel(
    notification: InAppNotificationService,
    private val repository: ClientUserRepository,
    private val sessionManager: AppSessionManager,
    private val telemetryClient: TelemetryLogClient,
    private val quotaGuard: WorkspaceResourceQuotaGuard,
    platformStore: PlatformRuntimeStore
) : BaseViewModel(notification) {

    val productType = sessionManager.activeProductType

    val organizationConfig = platformStore.organizationConfig

    private val _actionState = MutableStateFlow<UiResult<Any>>(UiResult.Idle)
    val actionState = _actionState.asStateFlow()

    val existingUsers = MutableStateFlow<List<UserItem>>(emptyList())

    val selectedUserId = MutableStateFlow<String?>(null)
    val selectedWorkspaceId = MutableStateFlow("")
    val selectedBranchId = MutableStateFlow("")
    val selectedRole = MutableStateFlow(TenantUserRole.OPERATOR.name)

    val useOrgCapabilities = MutableStateFlow(true)
    val selectedCapabilities = MutableStateFlow<Set<UserCapability>>(
        DefaultCapabilities.forRole(TenantUserRole.OPERATOR)
    )

    init {
        fetchExistingUsers()
    }

    fun fetchExistingUsers() {
        viewModelScope.launch {
            runCatching {
                val pageResult = repository.syncCloudPage(PageQuery(page = 0, size = 100))
                existingUsers.value = pageResult.items
            }
        }
    }

    fun onRoleSelected(roleStr: String) {
        selectedRole.value = roleStr
        selectedCapabilities.value = DefaultCapabilities.forRole(roleStr)
    }

    fun toggleCapability(capability: UserCapability) {
        val current = selectedCapabilities.value.toMutableSet()
        if (current.contains(capability)) {
            current.remove(capability)
        } else {
            current.add(capability)
        }
        selectedCapabilities.value = current
    }

    fun bindUser(onSuccess: () -> Unit) {
        val userId = selectedUserId.value ?: run {
            showError("Please select an existing user")
            return
        }
        val wsId = selectedWorkspaceId.value.takeIf { it.isNotBlank() } ?: run {
            showError("Please select a target workspace to bind")
            return
        }

        val targetBranch = selectedBranchId.value.takeIf { it.isNotBlank() }

        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            try {
                quotaGuard.consumeLocalResource(ResourceType.USER, workspaceId = wsId)

                val request = BindUserToWorkspaceRequest(
                    userId = userId,
                    workspaceId = wsId,
                    role = selectedRole.value,
                    capabilities = selectedCapabilities.value,
                    useOrgCapabilities = useOrgCapabilities.value,
                    branchId = targetBranch
                )

                repository.bindUserToWorkspace(request).getOrThrow()

                telemetryClient.trackApplicationEvent(
                    actorId = sessionManager.appUser.value?.id ?: "SYSTEM",
                    entityType = "USER",
                    entityId = userId,
                    action = "BIND_WORKSPACE",
                    summary = "Bound user $userId to workspace $wsId (branch: ${targetBranch ?: "N/A"}) with role ${selectedRole.value}"
                )

                _actionState.value = UiResult.Idle
                showSuccess("User successfully bound to workspace")
                onSuccess()
            } catch (e: Exception) {
                _actionState.value = UiResult.Error(e.message ?: "Failed to bind user to workspace")
            }
        }
    }
}
