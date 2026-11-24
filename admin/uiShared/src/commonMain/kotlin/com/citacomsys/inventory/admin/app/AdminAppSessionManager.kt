package com.citacomsys.inventory.admin.app

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.service.WorkStationInfo
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.ProductType
import com.citacomsys.types.User
import com.citacomsys.types.UserWorkspaceBinding
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdminAppSessionManager(
    private val prefs: KSafe,
    private val scope: CoroutineScope
) : AppSessionManager {
    override val appUser: StateFlow<User?> by prefs.asMutableStateFlow(null, scope, "user")
    override val organizationId: String? = appUser.value?.organizationId
    override val workStationInfo: StateFlow<WorkStationInfo> = MutableStateFlow(WorkStationInfo())
    override val currentUserId: String? = appUser.value?.id
    override val actorName: String? = appUser.value?.name
    override val deploymentMode: DeploymentMode? = DeploymentMode.CLOUD_SYNCED

    override val activeWorkspaceId: String? = null
    override val activeWorkspaceIdFlow: StateFlow<String?> = MutableStateFlow(null)
    override val activeViewBranchId: StateFlow<String?> = MutableStateFlow(null)
    override val writeTargetBranchId: StateFlow<String?> = MutableStateFlow(null)
    override val isBranchLocked: StateFlow<Boolean> = MutableStateFlow(false)
    override val isWorkspaceLocked: StateFlow<Boolean> = MutableStateFlow(false)
    override val userWorkspaceBindings: Flow<List<UserWorkspaceBinding>> =
        MutableStateFlow(emptyList())

    override suspend fun setAppUser(user: User?) {
        prefs.put("user", user)
    }

    override suspend fun getUser(): User? {
        return prefs.getDirect<User?>("user", null)
    }


    override suspend fun isAuthorized(workspaceId: String, branchId: String): String? = null
    override suspend fun setWorkspaceBindings(bindings: List<UserWorkspaceBinding>) {}

    override fun clearWorkspaceSession() {}

    override suspend fun configureWorkspace(
        workspaceId: String,
        defaultBranchId: String,
        type: ProductType,
        isBranchLocked: Boolean
    ) {}

    override fun resetWorkspaceSession() {
    }

    override fun setViewBranchId(branchId: String?) {}
    override fun setWriteBranchId(branchId: String) {}
    override fun setBranchLocked(locked: Boolean) {}
    override suspend fun setWorkspaceLocked(locked: Boolean, lockWsId: String) {}
    override suspend fun recordAccessedBranch(workspaceId: String, branchId: String) {}
    override fun getAccessedBranches(): Set<String> = emptySet()
    override fun resolveWriteBranchId(): String = ""
    override fun isSubscribed(): Boolean = true
    override fun isSubscribed(wsId: String): Boolean = true
    override fun isCloudDeployed(wsId: String): Boolean = true
}