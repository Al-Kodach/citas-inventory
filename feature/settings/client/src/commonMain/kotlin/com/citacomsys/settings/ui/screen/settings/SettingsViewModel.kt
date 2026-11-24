package com.citacomsys.settings.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import com.citacomsys.kernel.service.PlatformClientService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.citacomsys.types.NodeMeshMode

class SettingsViewModel(
    private val prefs: PreferencesRepository,
    private val syncPrefs: SyncPreferencesRepository,
    platformService: PlatformClientService,
    sessionManager: AppSessionManager,
    private val syncEngine: EntitySyncEngine,
    private val meshManager: MeshManager,
    private val connectivityService: NetworkConnectivityService,
    private val appUpdater: PlatformAppUpdater,
) : ViewModel() {

    val appUser = sessionManager.appUser
    val isDarkTheme = prefs.isDarkTheme
    val userFontScale = prefs.userFontScale
    val isOnline = connectivityService.connectionStatusFlow

    val meshSyncMode = syncPrefs.meshSyncMode
    val isMotherHubEnabled = syncPrefs.isMotherHubEnabled

    val updateState = appUpdater.syncState
    val currentVersion = appUpdater.currentVersion

    val branding = platformService.branding

    val isSyncing = syncEngine.isSyncing

    val syncMetadata: StateFlow<LastSyncMetadata?> = syncPrefs.observeLastSyncMetadata()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    fun triggerManualSync() {
        viewModelScope.launch {
            syncEngine.syncAll()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            appUpdater.checkForUpdates()
        }
    }

    fun startUpdateDownload() {
        appUpdater.startDownloading()
    }

    fun applyUpdate() {
        appUpdater.triggerInstallation()
    }

    fun toggleTheme() = prefs.toggleTheme()

    fun setFontScale(scale: Int) {
        prefs.setFontScale(scale)
    }

    fun setMeshSyncMode(mode: NodeMeshMode) {
        syncPrefs.setMeshSyncMode(mode)
    }

    fun setMotherHubEnabled(enabled: Boolean) {
        syncPrefs.setMotherHubEnabled(enabled)
    }
}
