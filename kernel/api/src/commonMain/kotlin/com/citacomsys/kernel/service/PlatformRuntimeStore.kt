package com.citacomsys.kernel.service

import com.citacomsys.kernel.model.BranchSummary
import com.citacomsys.kernel.model.LocalWorkstationProfile
import com.citacomsys.kernel.model.OrganizationRuntimeConfig
import com.citacomsys.kernel.model.WorkspaceRuntimeConfig
import com.citacomsys.kernel.model.WorkspaceRuntimeState
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.UserDeviceInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists and observes the runtime configuration for the entire platform,
 * from the parent Organization to the specific active Workspace.
 */
interface PlatformRuntimeStore {
    // Organization Context
    val organizationConfig: StateFlow<OrganizationRuntimeConfig?>
    val organizationId: String?
    val organizationName: String

    // Organization Devices Cache
    val orgDevices: StateFlow<List<UserDeviceInfo>>

    // Workspace Registry (Offline Switching)
    val workstationRegistry: StateFlow<Map<String, LocalWorkstationProfile>>

    fun getOrgConfig(): OrganizationRuntimeConfig?

    fun getRegistry(): Map<String, LocalWorkstationProfile>

    fun getOrgDevices(): List<UserDeviceInfo>

    /**
     * Helper to get a workspace info from the registered workstation profiles
     */
    fun getWorkspaceConfig(workspaceId: String): WorkspaceRuntimeConfig?
    fun getWorkspaceState(workspaceId: String): WorkspaceRuntimeState?

    fun getWorkspaceSummary(workspaceId: String): WorkspaceSummary?
    fun getBranchInfo(workspaceId: String, branchId: String): BranchSummary?
    fun getBrandingConfig(id: String): BrandingConfig?

    val brandingMap: StateFlow<Map<String, BrandingConfig>>

    fun saveBranding(id: String, branding: BrandingConfig)
    fun getBranding(id: String): BrandingConfig?

    suspend fun saveOrganizationConfig(config: OrganizationRuntimeConfig)

    suspend fun saveOrgDevices(devices: List<UserDeviceInfo>)

    suspend fun updateWorkspaceConfig(config: WorkspaceRuntimeConfig)
    suspend fun updateWorkspaceState(state: WorkspaceRuntimeState)

    suspend fun saveWorkstationProfile(profile: LocalWorkstationProfile)
    suspend fun removeWorkstationProfile(workspaceId: String)

    suspend fun clearAll()
    fun getProfile(workspaceId: String): LocalWorkstationProfile?
    suspend fun getWorkspaces(): List<WorkspaceSummary>
}
