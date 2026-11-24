package com.citacomsys.feature.workspace.ui.profile

import androidx.lifecycle.viewModelScope
import com.citacomsys.kernel.model.*
import com.citacomsys.api.workspace.model.WorkspaceSubscription
import com.citacomsys.api.workspace.service.WorkspaceApiService
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.types.UserCapability
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WorkspaceProfileUiState(
    val id: String,
    val name: String,
    val productKey: String? = null,
    val subscription: UiResult<WorkspaceSubscription>,
    val config: WorkspaceRuntimeConfig? = null,
    val canManageBilling: Boolean = false,
    val workspaceState: WorkspaceRuntimeState?
)

class WorkspaceProfileViewModel(
    private val api: WorkspaceApiService,
    notification: InAppNotificationService,
    private val workspaceService: PlatformClientService,
    capabilityManager: UserCapabilityManager,
    private val prefs: KSafe
) : BaseViewModel(notification) {

    private val _refreshState = MutableStateFlow<UiResult<Any>>(UiResult.Idle)
    val refreshState = _refreshState.asStateFlow()

    val workspaceConfig = workspaceService.activeWorkspaceConfig
    val workspaceState = workspaceService.activeWorkspaceState

    private val subscription by prefs.asMutableStateFlow<UiResult<WorkspaceSubscription>>(
        UiResult.Loading,
        viewModelScope
    )

    val state: StateFlow<UiResult<WorkspaceProfileUiState>> = combine(
        workspaceConfig,
        workspaceState,
        subscription,
        capabilityManager.capabilities
    ) { config, state, subs, capabilities ->
        when {
            config != null -> {
                UiResult.Success(
                    WorkspaceProfileUiState(
                        id = config.workspaceId,
                        name = config.name,
                        config = config,
                        subscription = subs,
                        canManageBilling = capabilities.contains(UserCapability.MANAGE_BILLING),
                        workspaceState = state
                    )
                )
            }

            else -> UiResult.Loading
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiResult.Loading
    )

    init {
        reloadSubscription()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = UiResult.Loading
            reloadSubscription()
            _refreshState.value = UiResult.Idle
        }
    }

    fun reloadSubscription() {
        viewModelScope.launch {
            subscription.value = UiResult.Loading
            subscription.value = api.getSubscription().asUiResult()
        }
    }

    fun updateIdentity(name: String, email: String?, phone: String?) {
        viewModelScope.launch {
            if (name.isBlank()) {
                showError("Business id is required")
                return@launch
            }

            api.updateProfile(name, email, phone).fold(
                onSuccess = { 
                    showSuccess("Business identity updated")
                    viewModelScope.launch { workspaceService.refreshWorkspaceConfig() }
                },
                onFailure = { showError(it.message ?: "Update failed") }
            )
        }
    }
}
