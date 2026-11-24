package com.citacomsys.kernel.client

import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.security.OfflineAuthenticator
import com.citacomsys.kernel.model.WorkspaceRuntimeState
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.NodeMeshMode

/**
 * Applies the enforcement policies dictated by the server and local runtime security context.
 */
class RuntimeSecurityHandler(
    private val platformRuntimeStore: PlatformRuntimeStore,
    private val tokenStore: TokenStore,
    private val preferencesRepository: SyncPreferencesRepository,
    private val offlineAuthenticator: OfflineAuthenticator,
) {
    suspend fun apply(state: WorkspaceRuntimeState) {
        val isWorkspaceDeviceBlocked =
            state.deviceStatus == DeviceStatus.LOCKED || state.deviceStatus == DeviceStatus.REVOKED

        val globalDevices = platformRuntimeStore.getOrgDevices()
        val isGlobalDeviceBlocked = globalDevices.any {
            it.status == DeviceStatus.LOCKED || it.status == DeviceStatus.REVOKED
        }

        val isDeviceBlocked = isWorkspaceDeviceBlocked || isGlobalDeviceBlocked

        if (isDeviceBlocked || state.enforcement.forceLogout) {
            tokenStore.clearUserSession()
        }

        if (state.enforcement.clearOfflineProfiles || isDeviceBlocked) {
            offlineAuthenticator.clearProfiles()
            platformRuntimeStore.removeWorkstationProfile(state.workspaceId)
        }

        if (state.enforcement.forceCloudOnly) {
            preferencesRepository.setMeshSyncMode(NodeMeshMode.NODE)
        }

        if (state.enforcement.offlineRevokedUserIds.isNotEmpty()) {
            offlineAuthenticator.revokeUsers(state.enforcement.offlineRevokedUserIds)
        }
    }
}
