package com.citacomsys.settings.ui.screen.software

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.database.InventoryDatabase
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SoftwareInfoViewModel(
    private val appUpdater: PlatformAppUpdater
) : ViewModel() {

    val updateState: StateFlow<CommonUpdateState> = appUpdater.syncState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CommonUpdateState.Idle)

    val currentVersion: String = appUpdater.currentVersion
    val schemaVersion: Long = InventoryDatabase.Schema.version

    fun checkForUpdates() {
        viewModelScope.launch {
            appUpdater.checkForUpdates()
        }
    }

    fun startDownloading() {
        appUpdater.startDownloading()
    }

    fun triggerInstallation() {
        appUpdater.triggerInstallation()
    }
}
