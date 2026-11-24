package com.citacomsys.core.data.session

import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.service.WorkStationInfo
import com.citacomsys.core.domain.service.WorkspaceSessionManager
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.AppOperatingMode
import com.citacomsys.device.service.DeviceService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.ProductType
import com.citacomsys.types.User
import com.citacomsys.types.UserType
import com.citacomsys.types.UserWorkspaceBinding
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.getStateFlow
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import com.citacomsys.core.notification.AlertService

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultAppSessionManager(
    private val store: PlatformRuntimeStore,
    private val vault: KSafe,
    private val syncPrefs: SyncPreferencesRepository,
    private val deviceService: DeviceService,
    private val workspaceSessionManager: WorkspaceSessionManager,
    private val scope: CoroutineScope,
    private val appManifest: AppManifest
) : AppSessionManager, WorkspaceSessionManager by workspaceSessionManager {
    private val activeUser = "_active_user"
    private val workspaceBindingsKey = "_active_workspace_bindings"
    private val accessedBranchesKey = "accessed_branches_registry"
    private val workspaceLockKey = "workspace_lock"

    private val deviceName = deviceService.deviceInfo.value.displayName

    private val wss
        get() = store.organizationConfig.value?.workspaces
            ?.associateBy { it.id } ?: emptyMap()

    override val appUser: StateFlow<User?> = vault.getStateFlow(activeUser, null, scope)
    override val currentUserId: String? get() = appUser.value?.id
    override val actorName: String? get() = appUser.value?.name
    override val organizationId: String? get() = appUser.value?.organizationId

    private val profile = combine(
        activeWorkspaceIdFlow,
        store.workstationRegistry
    ) { wsId, registry ->
        wsId?.let { registry[it] }
    }

    override val workStationInfo: StateFlow<WorkStationInfo> = combine(
        appManifest.operatingMode,
        profile,
        store.organizationConfig,
        activeWorkspaceIdFlow,
        writeTargetBranchId
    ) { mode, profile, orgConfig, activeWsId, targetBranchId ->
        val activeWsSummary = orgConfig?.workspaces?.firstOrNull { it.id == activeWsId }
        val branchSummary = activeWsSummary?.branches?.firstOrNull { it.id == targetBranchId }
        val branchName = branchSummary?.name?.substringAfter(activeWsSummary?.name ?: "")

        val appName = if (mode == AppOperatingMode.ORG) {
            orgConfig?.branding?.appName ?: orgConfig?.name
        } else {
            profile?.config?.branding?.appName ?: profile?.config?.name
        }

        val user = appUser.value
        val type = activeProductType

        WorkStationInfo(
            appName = appName ?: "Citas App",
            deviceName = deviceName,
            orgName = orgConfig?.name ?: "Citacom Systems LTD",
            activeWorkspaceName = activeWsSummary?.name ?: "Citacom Systems",
            activeBranchName = branchName ?: "",
            isOrgMode = mode == AppOperatingMode.ORG,
            username = user?.name ?: "",
            phone = user?.phone?.value,
            email = user?.email,
            activeProductType = type,
        )
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        WorkStationInfo()
    )

    val isPlatformOrOrgAdmin: Boolean
        get() = appUser.value?.let { user ->
            user.userType == UserType.SYSTEM ||
                    user.userType == UserType.PLATFORM ||
                    user.role.contains("OWNER", ignoreCase = true) ||
                    user.role.contains("ADMIN", ignoreCase = true)
        } ?: false

    override val userWorkspaceBindings: Flow<List<UserWorkspaceBinding>> =
        vault.getFlow(workspaceBindingsKey, emptyList())

    private var lockedWsId: String?
        get() = vault.getDirect("locked_workspace", null)
        set(value) = vault.putDirect("locked_workspace", value)

    override val isWorkspaceLocked: StateFlow<Boolean> =
        vault.getStateFlow(workspaceLockKey, false, scope)

    override suspend fun setAppUser(user: User?) {
        if (user == null) {
            vault.delete(activeUser)
        } else {
            vault.put(activeUser, user)
        }
    }

    override suspend fun configureWorkspace(
        workspaceId: String,
        defaultBranchId: String,
        type: ProductType,
        isBranchLocked: Boolean
    ) {
        Napier.d(tag = "SESSION_MANAGER") { "Configuring workspace $workspaceId" }

        workspaceSessionManager.configureWorkspace(
            workspaceId = workspaceId,
            defaultBranchId = defaultBranchId,
            type = type,
            isBranchLocked = isBranchLocked
        )

        appManifest.setOperatingMode(AppOperatingMode.WS)
        ensureWorkspaceSyncTarget(workspaceId, defaultBranchId)
    }

    private fun ensureWorkspaceSyncTarget(workspaceId: String, branchId: String) {
        val isLocked = isWorkspaceLocked.value
        val isSubscribed = isSubscribed(workspaceId)

        val newTarget = SyncTarget(
            workspaceId = workspaceId,
            branchId = branchId,
            isCloudDeployed = isSubscribed
        )

        if (isLocked && !lockedWsId.isNullOrBlank()) {
            val lockedSubscribed = isSubscribed(lockedWsId!!)
            val target = SyncTarget(
                workspaceId = lockedWsId!!,
                branchId = branchId,
                isCloudDeployed = lockedSubscribed,
            )
            val updatedConfig = syncPrefs.syncConfig.value.copy(
                targets = listOf(target)
            )
            syncPrefs.setSyncConfig(updatedConfig)
            Napier.i(
                tag = "DefaultAppSessionManager",
                message = "Device is LOCKED to $lockedWsId. Sync target locked strictly to $lockedWsId."
            )
            return
        }

        val currentConfig = syncPrefs.syncConfig.value
        val existingIndex = currentConfig.targets.indexOfFirst {
            it.workspaceId == workspaceId && it.branchId == branchId
        }
        val updatedTargets = if (existingIndex >= 0) {
            currentConfig.targets.toMutableList().apply { this[existingIndex] = newTarget }
        } else {
            currentConfig.targets + newTarget
        }

        if (updatedTargets != currentConfig.targets) {
            val updatedConfig = currentConfig.copy(
                targets = updatedTargets
            )
            syncPrefs.setSyncConfig(updatedConfig)

            if (existingIndex < 0) {
                val wsName = store.getWorkspaceConfig(workspaceId)?.name ?: "Store"
                AlertService.showInfo(
                    "Added $wsName to sync targets. You can manage or remove sync targets in Sync Policy."
                )
            }

            Napier.i(
                tag = "DefaultAppSessionManager",
                message = "Updated workspace target $workspaceId ($branchId) [Subscribed: $isSubscribed]. Total targets: ${updatedTargets.size}"
            )
        }
    }

    override suspend fun isAuthorized(workspaceId: String, branchId: String): String? {
        if (isPlatformOrOrgAdmin) {
            return null
        }

        val bindings = userWorkspaceBindings.first()
        val wsBinding = bindings.find { it.workspaceId == workspaceId && it.isActive }
            ?: return "You are not bound to access this Business."

        if (!wsBinding.branchId.isNullOrBlank() && wsBinding.branchId != branchId) {
            return "You are not authorized to access this specific Store."
        }

        if (isWorkspaceLocked.value && lockedWsId != workspaceId) {
            val lockedWorkspace = store.getWorkspaceConfig(workspaceId)?.name ?: "Unknown"
            return "This device is locked to $lockedWorkspace"
        }

        return null
    }

    override suspend fun setWorkspaceBindings(bindings: List<UserWorkspaceBinding>) {
        vault.put(workspaceBindingsKey, bindings)
    }

    override suspend fun setWorkspaceLocked(locked: Boolean, lockWsId: String) {
        vault.put(workspaceLockKey, locked)

        if (locked) {
            this.lockedWsId = lockWsId
            val activeBranch = writeTargetBranchId.value ?: ""
            val lockTarget = SyncTarget(workspaceId = lockWsId, branchId = activeBranch)
            val updatedConfig = syncPrefs.syncConfig.value.copy(
                targets = listOf(lockTarget)
            )
            syncPrefs.setSyncConfig(updatedConfig)
            Napier.i(
                tag = "DefaultAppSessionManager",
                message = "Device locked to workspace $lockWsId. Cleared all other sync targets."
            )
        } else {
            this.lockedWsId = null
        }
    }

    override suspend fun recordAccessedBranch(workspaceId: String, branchId: String) {
        val existing = vault.getDirect(accessedBranchesKey, emptyList<String>())
        val entry = "$workspaceId:$branchId"
        if (!existing.contains(entry)) {
            vault.putDirect(accessedBranchesKey, existing + entry)
        }
    }

    override fun getAccessedBranches(): Set<String> {
        return vault.getDirect(accessedBranchesKey, emptyList<String>()).toSet()
    }

    override suspend fun getUser(): User? {
        return vault.get(activeUser, null)
    }

    override fun resolveWriteBranchId(): String {
        return writeTargetBranchId.value ?: activeWorkspaceId ?: error("No active Store found")
    }

    // Active workspace deployment mode check
    override fun isSubscribed(): Boolean {
        val wsId = activeWorkspaceId ?: return false
        return store.getWorkspaceSummary(wsId)?.deploymentMode?.isCloudSynced == true
                || syncPrefs.activeHubIp.value.isNullOrBlank().not() // Satellite
    }

    // Relay workspace deployment mode check
    override fun isSubscribed(wsId: String): Boolean {
        // Check mesh mode
        if (wsId == activeWorkspaceId) return isSubscribed()

        // Check only deployment mode for relay
        return store.getWorkspaceSummary(wsId)
            ?.deploymentMode?.isCloudSynced == true
    }

    override fun isCloudDeployed(wsId: String): Boolean {
        return store.getWorkspaceSummary(wsId)?.deploymentMode?.isCloudSynced == true
    }
}
