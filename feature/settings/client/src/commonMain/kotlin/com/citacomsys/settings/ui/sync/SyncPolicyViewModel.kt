package com.citacomsys.settings.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.model.SyncConfig
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.domain.model.SyncTimingStrategy
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.kernel.service.PlatformClientService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TargetUi(
    val workspaceId: String,
    val branchId: String,
    val businessName: String,
    val branchName: String,
)

class SyncPolicyViewModel(
    private val sessionManager: AppSessionManager,
    private val syncPrefs: SyncPreferencesRepository,
    private val platformService: PlatformClientService,
) : ViewModel() {

    val activeWorkspaceName: String?
        get() {
            val wsId = sessionManager.activeWorkspaceId ?: return null
            val orgConfig = platformService.organizationConfig.value
            return orgConfig?.workspaces?.find { it.id == wsId }?.name
                ?: platformService.activeWorkspaceConfig.value?.name
                ?: wsId
        }

    val isSubscribed: Boolean
        get() = sessionManager.isSubscribed()

    val isMotherHubEnabled: StateFlow<Boolean> = syncPrefs.isMotherHubEnabled
    val syncConfig: StateFlow<SyncConfig> = syncPrefs.syncConfig

    val organizationWorkspaces: StateFlow<List<WorkspaceSummary>> =
        platformService.organizationConfig.map { it?.workspaces ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Automatically retrieves accessed branch history from session manager
     * and maps them to UI-ready TargetUi models for Standard (Standalone) Mode.
     */
    val accessedTargets: StateFlow<List<TargetUi>> =
        organizationWorkspaces.map { workspaces ->
            val rawTargets = sessionManager.getAccessedBranches().mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    SyncTarget(workspaceId = parts[0], branchId = parts[1])
                } else null
            }
            rawTargets.map { target ->
                val ws = workspaces.find { it.id == target.workspaceId }
                val branch = ws?.branches?.find { it.id == target.branchId }
                TargetUi(
                    workspaceId = target.workspaceId,
                    branchId = target.branchId,
                    businessName = ws?.name ?: "Business Entity",
                    branchName = branch?.name ?: "Store"
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    fun toggleSyncTarget(workspaceId: String, branchId: String, isSelected: Boolean) {
        val isSubscribed = sessionManager.isSubscribed(workspaceId)
        val current = syncPrefs.syncConfig.value
        val target = SyncTarget(
            workspaceId = workspaceId,
            branchId = branchId,
            isCloudDeployed = isSubscribed
        )
        val updatedTargets = if (isSelected) {
            val index = current.targets.indexOfFirst { it.workspaceId == workspaceId && it.branchId == branchId }
            if (index < 0) {
                current.targets + target
            } else {
                current.targets.toMutableList().apply { this[index] = target }
            }
        } else {
            current.targets.filterNot { it.workspaceId == workspaceId && it.branchId == branchId }
        }
        syncPrefs.setSyncConfig(current.copy(targets = updatedTargets))
    }

    fun setSyncTimingStrategy(strategy: SyncTimingStrategy) {
        val current = syncPrefs.syncConfig.value
        syncPrefs.setSyncConfig(current.copy(syncTimingStrategy = strategy))
    }

    fun setSyncIntervalMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceAtLeast(1)
        val current = syncPrefs.syncConfig.value
        syncPrefs.setSyncConfig(current.copy(syncIntervalMinutes = safeMinutes))
    }

    fun setAllowCellularSync(enabled: Boolean) {
        val current = syncPrefs.syncConfig.value
        syncPrefs.setSyncConfig(current.copy(allowCellularSync = enabled))
    }

    fun setRequiresCharging(required: Boolean) {
        val current = syncPrefs.syncConfig.value
        syncPrefs.setSyncConfig(current.copy(requiresCharging = required))
    }

    fun setRequiresDeviceIdle(required: Boolean) {
        val current = syncPrefs.syncConfig.value
        syncPrefs.setSyncConfig(current.copy(requiresDeviceIdle = required))
    }
}
