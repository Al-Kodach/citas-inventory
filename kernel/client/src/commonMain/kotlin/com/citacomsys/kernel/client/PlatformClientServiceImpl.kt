package com.citacomsys.kernel.client

import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.service.SystemEventService
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.kernel.model.*
import com.citacomsys.kernel.service.KernelApiService
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.*
import com.citacomsys.shared.util.mapStateFlow
import com.citacomsys.types.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal class PlatformClientServiceImpl(
    private val kernelApiService: KernelApiService,
    private val store: PlatformRuntimeStore,
    private val sessionManager: AppSessionManager,
    private val environmentManager: WorkspaceEnvironmentManager,
    private val orgConfigStateManager: OrganizationConfigStateManager,
    private val securityHandler: RuntimeSecurityHandler,
    private val systemEventService: SystemEventService,
    private val tokenStore: TokenStore,
    private val preferencesRepository: PreferencesRepository,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : PlatformClientService {

    private val TAG = "PLATFORM_KERNEL"

    private val refreshMutex = Mutex()

    private var serverTimeDrift: Long = 0

    private val activeWorkspaceId get() = sessionManager.activeWorkspaceId

    // Organization Context
    override val organizationConfig: StateFlow<OrganizationRuntimeConfig?> =
        store.organizationConfig

    // Workspace Context - Reacts to active workspace ID and background workstation registry updates
    private val activeProfile = combine(
        sessionManager.activeWorkspaceIdFlow,
        store.workstationRegistry
    ) { wsId, profiles ->
        profiles[wsId] ?: store.getRegistry()[wsId]
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        null
    )

    override val activeWorkspaceConfig: StateFlow<WorkspaceRuntimeConfig?> =
        activeProfile.map { it?.config }.stateIn(
            scope,
            SharingStarted.Eagerly,
            null
        )

    override val resource: StateFlow<WorkspaceResource?> =
        activeWorkspaceConfig.mapStateFlow(scope) { it?.resource }

    override val activeWorkspaceState: StateFlow<WorkspaceRuntimeState?> =
        activeProfile.map { it?.state }.stateIn(
            scope,
            SharingStarted.Eagerly,
            null
        )

    override val branding: StateFlow<BrandingConfig?> = combine(
        activeProfile,
        store.organizationConfig,
        sessionManager.workStationInfo,
        preferencesRepository.useOrgBranding,
        preferencesRepository.usePlatformBranding
    ) { profile, orgConfig, info, useOrg, usePlatform ->
        when {
            usePlatform -> null
            info.isOrgMode || useOrg -> orgConfig?.branding
            else -> profile?.config?.branding ?: orgConfig?.branding
        }
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        null
    )

    private var _moduleState: Map<ProductModule, WorkspaceProductModuleState> = emptyMap()

    private val _serverPolicy = MutableStateFlow<ServerProductPolicy?>(null)

    override val modulesMap: StateFlow<Map<ProductModule, EffectiveModulePolicy>> =
        combine(activeWorkspaceConfig, _serverPolicy) { config, policy ->
            merge(config?.resource?.modules ?: emptySet(), policy)
        }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    init {
        // Publish branding changes to PreferencesRepository for decoupled lightweight UI theme consumption
        scope.launch {
            branding.collect { newBranding ->
                preferencesRepository.setActiveBranding(newBranding)
            }
        }

        // Global Hardware Revocation / Attack Detection (Hardware Blacklisted)
        scope.launch {
            merge(
                EventBus.subscribe<DeviceRevoked>(),
                EventBus.subscribe<DeviceFingerprintMismatch>(),
                EventBus.subscribe<DebugAttachedDetected>(),
                EventBus.subscribe<DeviceSecurityEvent>()
            ).collect { event ->
                val isGlobalRevoke = when (event) {
                    is DeviceSecurityEvent -> event.action == "REVOKE" || event.action == "DELETE"
                    else -> true
                }
                if (isGlobalRevoke) {
                    Napier.e(tag = TAG) { "Global hardware revocation detected ($event). Clearing all stores & logging out." }
                    store.clearAll()
                    tokenStore.clearAll()
                    sessionManager.setAppUser(null)
                    sessionManager.setWorkspaceBindings(emptyList())
                    EventBus.publish(AppLifecycleEvent.UserSessionRevoked)
                    EventBus.publish(AppLifecycleEvent.ExistWorkspace)
                }
            }
        }

        // Specific Workstation Binding Unbind / Revoke (Store Unbound)
        scope.launch {
            EventBus.subscribe<WorkstationBindingEvent>().collect { event ->
                if (event.action == "UNBIND" || event.action == "DELETE" || event.action == "REVOKE") {
                    val targetWsId = event.workspaceId
                    Napier.w(tag = TAG) { "Workstation unbind event received for workspace: $targetWsId" }

                    if (!targetWsId.isNullOrBlank()) {
                        // Mark device status REVOKED for that specific workspace profile
                        val profile = store.getProfile(targetWsId)
                        if (profile != null) {
                            store.saveWorkstationProfile(profile.copy(deviceStatus = DeviceStatus.REVOKED))
                        }

                        // If active workspace is the one being unbound -> Exit workspace to Workspace Selection
                        if (sessionManager.activeWorkspaceId == targetWsId) {
                            Napier.w(tag = TAG) { "Active workspace $targetWsId was unbound. Exiting workspace to selection screen." }
                            EventBus.publish(AppLifecycleEvent.ExistWorkspace)
                        }
                    }
                }
            }
        }

        // User Session Revocation
        scope.launch {
            EventBus.subscribe<UserSessionRevokedEvent>().collect { event ->
                Napier.w(tag = TAG) { "User session revoked event received for user ${event.userId}. Logging out." }
                tokenStore.clearAll()
                EventBus.publish(AppLifecycleEvent.UserSessionRevoked)
            }
        }

        // Control SSE Stream and update subscriptions when active workspace or branch changes
        scope.launch {
            combine(
                sessionManager.activeWorkspaceIdFlow,
                sessionManager.writeTargetBranchId
            ) { wsId, branchId ->
                wsId to branchId
            }.collect { (wsId, branchId) ->
                if (!wsId.isNullOrBlank()) {
                    Napier.i(tag = TAG) { "Kernel updating SSE subscription for Workspace: $wsId, Branch: $branchId" }
                    systemEventService.updateSubscription(
                        topics = setOf("INVENTORY_STOCK", "SUBSCRIPTION", "BILLING", "SECURITY"),
                        workspaceId = wsId,
                        branchId = branchId
                    )
                }
            }
        }

        // Observe active workspace state changes and enforce runtime security policy rules
        scope.launch {
            activeWorkspaceState.filterNotNull().collect { state ->
                securityHandler.apply(state)
            }
        }

        startRuntimeServices()
    }

    private fun startRuntimeServices() {
        scope.launch {
            modulesMap.collect { policy ->
                _moduleState =
                    policy.values.map { it.toRuntimeState() }.associateBy { it.module }
            }
        }
    }

    override fun canWrite(module: ProductModule): Boolean {
        val state = activeWorkspaceState.value ?: return true

        if (!state.enforcement.allowWrites) {
            Napier.w(tag = TAG) { "State is not allowing writes for ${module.displayName()}" }
            return false
        }

        if (state.deviceStatus == DeviceStatus.LOCKED || state.deviceStatus == DeviceStatus.REVOKED) {
            Napier.w(tag = TAG) { "Write Denied: Device status is ${state.deviceStatus}" }
            return false
        }

        val modulePolicy = _moduleState[module]

        if (modulePolicy == null) {
            Napier.d(tag = TAG) { "No explicit module policy found for ${module.displayName()}. Defaulting to allowWrites." }
            return true
        }

        return modulePolicy.allowWrites
    }

    override suspend fun refreshOrganizationConfig(): Result<Unit> {
        return orgConfigStateManager.refreshOrgConfig(force = true)
    }

    override suspend fun refreshWorkspaceConfig(): Result<Unit> {
        return environmentManager.refreshAllRegisteredWorkspaces()
    }

    override suspend fun refreshWorkspaceConfig(workspaceId: String): Result<Unit> {
        return environmentManager.refreshWorkspace(workspaceId)
    }


    override suspend fun refreshWorkspaceState(): Result<Unit> {
        return environmentManager.refreshAllRegisteredWorkspaces()
    }

    private fun onPolicyVersionReceived(incomingVersion: Long) {
        val currentVersion = _serverPolicy.value?.version ?: 0L
        if (incomingVersion > currentVersion) {
            scope.launch { refreshPolicy(currentVersion) }
        }
    }

    private suspend fun refreshPolicy(currentVersion: Long? = null) = refreshMutex.withLock {
        try {
            _serverPolicy.value = kernelApiService.getModulePolicy(currentVersion)
        } catch (e: Exception) {
            Napier.e(e, tag = TAG) { "Policy refresh failed" }
        }
    }

    private fun merge(
        modules: Set<ProductModule>,
        policy: ServerProductPolicy?
    ): Map<ProductModule, EffectiveModulePolicy> {
        val activeModules = if (modules.isEmpty()) ProductModule.entries.toSet() else modules
        val base = activeModules.map { EffectiveModulePolicy(it) }.associateBy { it.module }

        if (policy == null) return base
        val policyMap = policy.modules.associateBy { it.module }

        return base.mapValues { (id, feature) ->
            val override = policyMap[id]
            if (override != null) feature.copy(
                status = override.status,
                reason = override.reason
            ) else feature
        }
    }
}
