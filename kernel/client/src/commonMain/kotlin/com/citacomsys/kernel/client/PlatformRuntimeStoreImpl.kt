package com.citacomsys.kernel.client

import com.citacomsys.kernel.model.BranchSummary
import com.citacomsys.kernel.model.LocalWorkstationProfile
import com.citacomsys.kernel.model.OrganizationRuntimeConfig
import com.citacomsys.kernel.model.WorkspaceRuntimeConfig
import com.citacomsys.kernel.model.WorkspaceRuntimeState
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.UserDeviceInfo
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import eu.anifantakis.lib.ksafe.getStateFlow
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyMap

internal class PlatformRuntimeStoreImpl(
    private val vault: KSafe,
    private val prefs: KSafe,
    private val scope: CoroutineScope,
) : PlatformRuntimeStore {
    private val orgConfigKey = "_organization_runtime_config"
    private val workstationRegistryKey = "workstation_registry"
    private val orgDevicesKey = "_org_devices_cache"
    private val brandingKey = "_branding_config"

    private val _branding by prefs.asMutableStateFlow(
        emptyMap<String, BrandingConfig>(),
        scope,
        brandingKey
    )
    override val brandingMap: StateFlow<Map<String, BrandingConfig>> = _branding.asStateFlow()

    // Organization Context
    override val organizationConfig: StateFlow<OrganizationRuntimeConfig?> =
        vault.getStateFlow(orgConfigKey, null, scope)

    override val organizationId: String? = organizationConfig.value?.organizationId

    override val organizationName: String
        get() = organizationConfig.value?.name ?: "Citacom Systems LTD"

    override val orgDevices: StateFlow<List<UserDeviceInfo>> =
        vault.getStateFlow(orgDevicesKey, emptyList(), scope)

    override val workstationRegistry: StateFlow<Map<String, LocalWorkstationProfile>> =
        vault.getStateFlow(workstationRegistryKey, emptyMap(), scope)

    override suspend fun saveOrganizationConfig(config: OrganizationRuntimeConfig) {
        vault.put(orgConfigKey, config)
    }

    override suspend fun saveOrgDevices(devices: List<UserDeviceInfo>) {
        vault.put(orgDevicesKey, devices)
    }

    override fun getOrgDevices(): List<UserDeviceInfo> {
        return orgDevices.value.ifEmpty { vault.getDirect(orgDevicesKey, emptyList()) }
    }

    override fun getRegistry(): Map<String, LocalWorkstationProfile> {
        return workstationRegistry.value.ifEmpty {
            vault.getDirect(
                workstationRegistryKey,
                emptyMap()
            )
        }
    }

    override suspend fun saveWorkstationProfile(profile: LocalWorkstationProfile) {
        val current = getRegistry().toMutableMap()

        current[profile.workspaceId] = profile
        vault.put(workstationRegistryKey, current)
    }

    override suspend fun removeWorkstationProfile(workspaceId: String) {
        val current = getRegistry().toMutableMap()

        current.remove(workspaceId)
        vault.put(workstationRegistryKey, current)
    }

    override suspend fun updateWorkspaceConfig(config: WorkspaceRuntimeConfig) {
        val current = getRegistry().toMutableMap()
        val profile =
            current[config.workspaceId] ?: error("Profile not found for ${config.workspaceId}")

        current[config.workspaceId] = profile.copy(config = config)
        vault.put(workstationRegistryKey, current)
    }

    override suspend fun updateWorkspaceState(state: WorkspaceRuntimeState) {
        val registry = getRegistry()
        val profile =
            registry[state.workspaceId] ?: error("Profile not found for ${state.workspaceId}")
        val current = registry.toMutableMap()

        current[state.workspaceId] = profile.copy(state = state)
        vault.put(workstationRegistryKey, current)
    }

    override fun getOrgConfig(): OrganizationRuntimeConfig? {
        return organizationConfig.value ?: vault.getDirect(orgConfigKey, null)
    }

    override fun getWorkspaceConfig(workspaceId: String): WorkspaceRuntimeConfig? {
        return workstationRegistry.value[workspaceId]?.config ?: getRegistry()[workspaceId]?.config
    }

    override fun getProfile(workspaceId: String): LocalWorkstationProfile? {
        return workstationRegistry.value[workspaceId] ?: getRegistry()[workspaceId]
    }

    override fun getWorkspaceState(workspaceId: String): WorkspaceRuntimeState? {
        return workstationRegistry.value[workspaceId]?.state ?: getRegistry()[workspaceId]?.state
    }

    override fun getWorkspaceSummary(workspaceId: String): WorkspaceSummary? {
        return organizationConfig.value?.workspaces?.firstOrNull { it.id == workspaceId }
    }

    override fun getBranchInfo(workspaceId: String, branchId: String): BranchSummary? {
        val workspace = organizationConfig.value?.workspaces?.firstOrNull { it.id == workspaceId }
            ?: return null
        return workspace.branches.firstOrNull { it.id == branchId }
    }

    override fun getBrandingConfig(id: String): BrandingConfig? {
        return workstationRegistry.value[id]?.config?.branding
            ?: organizationConfig.value?.branding
    }

    override fun saveBranding(id: String, branding: BrandingConfig) {
        val current = _branding.value.toMutableMap()
        current[id] = branding
        _branding.value = current

        val profile = workstationRegistry.value[id]
        if (profile != null) {
            val updatedConfig = profile.config.copy(branding = branding)
            scope.launch {
                updateWorkspaceConfig(updatedConfig)
            }
        }

        val org = organizationConfig.value
        if (org != null && org.organizationId == id) {
            val updatedOrg = org.copy(branding = branding)
            scope.launch {
                saveOrganizationConfig(updatedOrg)
            }
        }
    }

    override fun getBranding(id: String): BrandingConfig? {
        return brandingMap.value[id]
    }

    override suspend fun clearAll() {
        Napier.w(tag = "TENANT_STORE") { "Clearing all config and state" }
        vault.delete(orgConfigKey)
        vault.delete(orgDevicesKey)
        vault.delete(workstationRegistryKey)
    }

    override suspend fun getWorkspaces(): List<WorkspaceSummary> {
        return organizationConfig.value?.workspaces ?: vault.getDirect<OrganizationRuntimeConfig?>(
            orgConfigKey,
            null
        )?.workspaces ?: emptyList()
    }
}
