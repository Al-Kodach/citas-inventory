package com.citacomsys.app.shared.app

import com.citacomsys.app.shared.boot.AppPlatform
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.AppOperatingMode
import com.citacomsys.core.spi.NavigationState
import com.citacomsys.kernel.model.OrganizationRuntimeConfig
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.DebugAttachedDetected
import com.citacomsys.shared.event.DeviceRevoked
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.User
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

enum class AppStage {
    INITIALIZING,            // System and core feature installation
    WELCOME,                 // Landing / Welcome screen
    AUTHENTICATION,          // User authentication

    W_SPACE_CONFIGURATION,  // Environment preparation, configuration and state update
    RESOLVING_IDENTITY,     // Downloading organization and workspace configuration
    MAIN_APP;               // Main application entry point

    val isInitState: Boolean
        get() = this == INITIALIZING || this == RESOLVING_IDENTITY
}

const val TAG = "APP_STAGE_ORCHESTRATOR"

@OptIn(FlowPreview::class)
class AppStageOrchestrator(
    tokenStore: TokenStore,
    store: PlatformRuntimeStore,
    private val sessionManager: AppSessionManager,
    private val scope: CoroutineScope,
    private val manifest: AppManifest
) {
    private val _stageFlow = MutableStateFlow(AppStage.INITIALIZING)

    // Debounced and stabilized stage flow to eliminate rapid UI transition thrashing
    val currentStage: StateFlow<AppStage> = _stageFlow
        .debounce(100.milliseconds)
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            AppStage.INITIALIZING
        )

    private val navStates = mutableMapOf<AppOperatingMode, NavigationState?>()
    private var isCoreReady: Boolean = false
    private var hasCompletedInitializations: Boolean = false
    private var lastNavWorkspaceId: String? = null

    var workspaceSelectEvent: AppLifecycleEvent.WorkspaceSelected? = null

    val activeNavState = manifest.operatingMode.map {
        navStates[it]
    }.stateIn(
        scope,
        started = SharingStarted.Eagerly,
        null
    )

    // Unified Reactive Initialization Engine - Debounced for snapshot stability
    private val userFlow = combine(
        tokenStore.accessToken,
        sessionManager.appUser,
        store.organizationConfig,
    ) { token, user, org ->
        UserSnapshot(
            token,
            user,
            org,
        )
    }.debounce(100.milliseconds)

    init {
        scope.launch {
            sessionManager.activeWorkspaceIdFlow.collect { wsId ->
                if (!wsId.isNullOrBlank() && lastNavWorkspaceId == null) {
                    lastNavWorkspaceId = wsId
                }
            }
        }

        scope.launch {
            userFlow.collect { user ->
                evaluateState(user)
            }
        }

        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.ExistWorkspace>().collect {
                logoutWorkspace()
            }
        }

        scope.launch {
            merge(
                EventBus.subscribe<DeviceRevoked>(),
                EventBus.subscribe<DebugAttachedDetected>()
            ).collect {
                Napier.e { "Device security violation detected. Resetting to Onboarding." }
            }
        }

        scope.launch {
            AppPlatform.lifecycleState.collect { lifecycle ->
                when (lifecycle) {
                    AppLifecycleEvent.Bootstrapping, AppLifecycleEvent.PostOnboarding -> setStage(AppStage.INITIALIZING)
                    AppLifecycleEvent.ForceUpdateRequired -> {
                        Napier.w(tag = TAG) { "Mandatory platform update required. Holding in INITIALIZING update gate." }
                        setStage(AppStage.INITIALIZING)
                    }

                    AppLifecycleEvent.CoreReady -> {
                        isCoreReady = true
                        evaluate()
                    }

                    else -> {}
                }
            }
        }

        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.WorkspaceSelected>().collect { event ->
                Napier.d(tag = TAG) { "Workspace selection event received for ${event.workspaceId}" }
                if (lastNavWorkspaceId != null && lastNavWorkspaceId != event.workspaceId) {
                    Napier.i(tag = TAG) { "Selected workspace changed ($lastNavWorkspaceId -> ${event.workspaceId}). Clearing WS nav state." }
                    navStates.remove(AppOperatingMode.WS)
                }
                lastNavWorkspaceId = event.workspaceId
                workspaceSelectEvent = event
                setStage(AppStage.W_SPACE_CONFIGURATION)
            }
        }

        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.WorkspaceConfigCompleted>().collect {
                workspaceConfigurationCompleted()
            }
        }

        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.WorkspaceRestore>().collect { event ->
                if (lastNavWorkspaceId != null && lastNavWorkspaceId != event.workspaceId) {
                    Napier.i(tag = TAG) { "Restoring different workspace ($lastNavWorkspaceId -> ${event.workspaceId}). Clearing WS nav state." }
                    navStates.remove(AppOperatingMode.WS)
                }
                lastNavWorkspaceId = event.workspaceId
                sessionManager.setBranchLocked(event.isBranchLocked)
                sessionManager.setWorkspaceLocked(event.isWsLocked, event.workspaceId)
                sessionManager.recordAccessedBranch(event.workspaceId, event.branchId)
                switchAppMode()
            }
        }

        // Fast switch between workspace and organization
        scope.launch {
            EventBus.subscribeAll(
                AppLifecycleEvent.OrganizationActive,
                AppLifecycleEvent.WorkspaceActive
            ).collect { event ->
                Napier.d(tag = TAG) { "Switching App Mode to ${if (event is AppLifecycleEvent.OrganizationActive) "Organization" else "Workspace"}" }
                switchAppMode()
            }
        }
    }

    /**
     * Completely logout / delete active workspace session
     */
    fun logoutWorkspace() {
        manifest.setOperatingMode(AppOperatingMode.ORG)
        sessionManager.clearWorkspaceSession()
        navStates.remove(AppOperatingMode.WS)
        lastNavWorkspaceId = null
    }

    fun switchAppMode() {
        val currentAppMode = manifest.operatingMode.value
        val nextMode = if (currentAppMode == AppOperatingMode.ORG)
            AppOperatingMode.WS else AppOperatingMode.ORG

        manifest.setOperatingMode(nextMode)
        setStage(AppStage.MAIN_APP)
    }

    fun recordNavState(state: NavigationState, mode: AppOperatingMode) {
        Napier.d(tag = TAG) { "Saving stacks in use for: ${mode.name} - ${state.stacksInUse}" }
        navStates[mode] = state
    }

    private fun evaluateState(user: UserSnapshot) {
        if (isCoreReady.not()) return

        handleAuthenticationState(user) ?: return

        scope.launch {
            if (hasCompletedInitializations) return@launch

            val postOnboardingResult = AppPlatform.triggerPostOnboardingPhase()
            if (postOnboardingResult.isFailure) {
                Napier.e(tag = TAG) { "Post-onboarding stage failed: ${postOnboardingResult.exceptionOrNull()?.message}" }
                hasCompletedInitializations = false
                return@launch
            }

            val workstationResult = AppPlatform.triggerWorkstationPhase()
            if (workstationResult.isFailure) {
                Napier.e(tag = TAG) { "Workstation stage failed: ${workstationResult.exceptionOrNull()?.message}" }
                hasCompletedInitializations = false
                return@launch
            }

            setStage(AppStage.MAIN_APP)
            hasCompletedInitializations = true

            Napier.i(tag = TAG) { ">>> All runtime stages completed cleanly. Transitioning to MAIN_APP" }
        }
    }

    private fun handleAuthenticationState(user: UserSnapshot): Unit? {
        val hasSession = !user.accessToken.isNullOrBlank()
        val orgConfig = user.organizationConfig

        if (!hasSession && orgConfig == null) {
            if (_stageFlow.value != AppStage.WELCOME) {
                Napier.i(tag = TAG) { "No active user session and org config found. Routing to Welcome." }
                setStage(AppStage.WELCOME)
            }
            return null
        }

        if ((!hasSession) && (orgConfig != null)) {
            if (_stageFlow.value != AppStage.AUTHENTICATION) {
                Napier.i(tag = TAG) { "No active user session found. Routing to Authentication." }
                setStage(AppStage.AUTHENTICATION)
            }
            return null
        }

        if (hasSession && (orgConfig == null)) {
            if (_stageFlow.value != AppStage.RESOLVING_IDENTITY) {
                Napier.i(tag = TAG) { "Active user session found. Resolving organization configuration." }
                setStage(AppStage.RESOLVING_IDENTITY)
                // Proceed to post onboarding to download org config
            }
        }

        return Unit
    }

    fun setStage(stage: AppStage) {
        if (_stageFlow.value == stage) return
        Napier.i(tag = TAG) { ">>> Transitioning Stage: ${_stageFlow.value} -> $stage" }
        _stageFlow.value = stage
    }

    fun workspaceConfigurationCompleted() {
        val currentWsId = sessionManager.activeWorkspaceId
        if (!currentWsId.isNullOrBlank() && lastNavWorkspaceId != null && lastNavWorkspaceId != currentWsId) {
            Napier.i(tag = TAG) { "Workspace changed on configuration completed ($lastNavWorkspaceId -> $currentWsId). Clearing WS nav state." }
            navStates.remove(AppOperatingMode.WS)
            lastNavWorkspaceId = currentWsId
        }
        Napier.d(tag = TAG) { "Workspace configuration completed. Switching App Mode." }
        setStage(AppStage.MAIN_APP)
    }

    fun evaluate() {
        scope.launch {
            evaluateState(userFlow.first())
        }
    }
}

private data class UserSnapshot(
    val accessToken: String?,
    val appUser: User?,
    val organizationConfig: OrganizationRuntimeConfig?
)
