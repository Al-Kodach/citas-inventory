package com.citacomsys.user.ui.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.inventory.api.user.model.BindUserToWorkspaceRequest
import com.citacomsys.inventory.api.user.model.SaveTenantUserRequest
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.DefaultCapabilities
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.ResourceType
import com.citacomsys.types.TenantUserRole
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserItem
import com.citacomsys.types.generateUUID
import com.citacomsys.user.data.ClientUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RegistrationMode {
    CREATE_NEW,
    ASSIGN_EXISTING
}

class UserFormViewModel(
    notification: InAppNotificationService,
    private val repository: ClientUserRepository,
    private val sessionManager: AppSessionManager,
    private val telemetryClient: TelemetryLogClient,
    private val quotaGuard: WorkspaceResourceQuotaGuard,
    platformStore: PlatformRuntimeStore,
) : BaseViewModel(notification) {
    val organizationConfig = platformStore.organizationConfig

    private val _actionState = MutableStateFlow<UiResult<Any>>(UiResult.Idle)
    val actionState = _actionState.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()

    // --- Dual Registration Mode ---
    val registrationMode = MutableStateFlow(RegistrationMode.CREATE_NEW)
    val existingUsers = MutableStateFlow<List<UserItem>>(emptyList())
    val selectedExistingUserId = MutableStateFlow<String?>(null)

    // --- Form State ---
    val name = MutableStateFlow("")
    val username = MutableStateFlow("")
    val phone = MutableStateFlow("")
    val email = MutableStateFlow("")
    val selectedRole = MutableStateFlow(TenantUserRole.OPERATOR.name)

    val selectedCapabilities = MutableStateFlow<Set<UserCapability>>(
        DefaultCapabilities.forRole(TenantUserRole.OPERATOR)
    )

    val selectedWorkspaceId = MutableStateFlow("")
    val selectedBranchId = MutableStateFlow("")

    val password = MutableStateFlow("")
    val confirmPassword = MutableStateFlow("")

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

    fun loadUser(userId: String?) {
        if (userId == null) {
            _isEditing.value = false
            return
        }

        viewModelScope.launch {
            _isEditing.value = true
            repository.findByIdRemote(userId).onSuccess { user ->
                name.value = user.name
                username.value = user.username
                phone.value = user.phone.value
                email.value = user.email ?: ""
                selectedRole.value = user.role
                selectedCapabilities.value = if (user.capabilities.isNotEmpty()) {
                    user.capabilities
                } else {
                    DefaultCapabilities.forRole(user.role)
                }
            }
        }
    }

    fun save(
        currentUserId: String? = null,
        onSuccess: () -> Unit
    ) {
        if (registrationMode.value == RegistrationMode.ASSIGN_EXISTING) {
            assignExistingUser(onSuccess)
            return
        }

        val currentWsId = selectedWorkspaceId.value.takeIf { it.isNotBlank() }
        val targetBranch = selectedBranchId.value.takeIf { it.isNotBlank() }

        viewModelScope.launch {
            _actionState.value = UiResult.Loading

            val request = if (_isEditing.value) {
                SaveTenantUserRequest(
                    id = currentUserId ?: generateUUID(),
                    name = name.value,
                    username = username.value,
                    phone = PhoneNumber(phone.value),
                    email = email.value.takeIf { it.isNotBlank() },
                    role = selectedRole.value,
                    capabilities = selectedCapabilities.value,
                    branchId = targetBranch,
                    wsId = currentWsId
                )
            } else {
                SaveTenantUserRequest(
                    name = name.value,
                    username = username.value,
                    phone = PhoneNumber(phone.value),
                    email = email.value.takeIf { it.isNotBlank() },
                    role = selectedRole.value,
                    capabilities = selectedCapabilities.value,
                    password = password.value,
                    branchId = targetBranch,
                    wsId = currentWsId
                )
            }

            try {
                val isNew = !_isEditing.value
                repository.save(request = request)

                if (isNew && currentWsId != null) {
                    quotaGuard.consumeLocalResource(ResourceType.USER, workspaceId = currentWsId)
                }

                val actionLabel = if (_isEditing.value) "UPDATE" else "CREATE"
                telemetryClient.trackApplicationEvent(
                    actorId = sessionManager.appUser.value?.id ?: "SYSTEM",
                    entityType = "USER",
                    entityId = request.id,
                    action = actionLabel,
                    summary = "${if (actionLabel == "CREATE") "Created" else "Updated"} staff member: ${request.name} (Workspace: ${currentWsId ?: "UNBOUND"}, Branch: ${request.branchId ?: "N/A"})"
                )

                _actionState.value = UiResult.Idle
                showSuccess("Staff member saved successfully")
                onSuccess()
            } catch (e: Exception) {
                _actionState.value = e.asUiError()
            }
        }
    }

    private fun assignExistingUser(onSuccess: () -> Unit) {
        val actor = sessionManager.appUser.value ?: return showError("Please login to continue")
        val userId = selectedExistingUserId.value
            ?: return showError("Please select an existing user to bind")

        val currentWsId = selectedWorkspaceId.value.takeIf { it.isNotBlank() }
            ?: return showError("Please select a target workspace to bind the user")

        val targetBranch = selectedBranchId.value.takeIf { it.isNotBlank() }

        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            try {
                val request = BindUserToWorkspaceRequest(
                    userId = userId,
                    workspaceId = currentWsId,
                    role = selectedRole.value,
                    capabilities = selectedCapabilities.value,
                    useOrgCapabilities = false,
                    branchId = targetBranch
                )

                repository.bindUserToWorkspace(request).getOrThrow()

                telemetryClient.trackApplicationEvent(
                    actorId = actor.id,
                    entityType = "USER",
                    entityId = userId,
                    action = "BIND_WORKSPACE",
                    summary = "Bound existing user $userId to workspace $currentWsId (branch: ${targetBranch ?: "N/A"}) with role ${selectedRole.value}"
                )

                _actionState.value = UiResult.Idle
                showSuccess("Existing user bound to workspace successfully")
                onSuccess()
            } catch (e: Exception) {
                _actionState.value = UiResult.Error(e.message ?: "Failed to bind existing user")
            }
        }
    }

    fun setUserId(id: String?) {
        loadUser(id)
    }
}
