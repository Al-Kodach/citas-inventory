package com.citacomsys.feature.telemetry.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ForceUpdateViewModel(
    private val updater: PlatformAppUpdater
) : ViewModel() {

    val updateState: StateFlow<CommonUpdateState> = updater.syncState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CommonUpdateState.Idle)

    val currentVersion = updater.currentVersion

    fun checkForUpdates(onNoUpdate: () -> Unit) {
        viewModelScope.launch {
            Napier.d { "ForceUpdateViewModel: Checking app update..." }
            val result = updater.checkForUpdates()
            
            if (result.isSuccess) {
                val response = result.getOrNull()
                Napier.d { "ForceUpdateViewModel: Check success. Mandatory -> ${response?.isMandatoryForceUpdate}" }

                if (response == null || !response.isMandatoryForceUpdate) {
                    Napier.d { "ForceUpdateViewModel: No mandatory update found. Proceeding." }
                    onNoUpdate()
                } else {
                    Napier.i { "ForceUpdateViewModel: Mandatory update v${response.version} required. Locking access." }
                    startDownloading()
                }
            } else {
                val error = result.exceptionOrNull()
                Napier.w { "ForceUpdateViewModel: Check failed: ${error?.message}. Allowing offline bypass." }
                // If check fails (e.g. offline), we allow proceeding to enable offline work
                onNoUpdate()
            }
        }
    }

    fun startDownloading() {
        Napier.i { "ForceUpdateViewModel: Starting download..." }
        updater.startDownloading()
    }

    fun triggerInstallation() {
        viewModelScope.launch {
            Napier.i { "ForceUpdateViewModel: Triggering installation..." }
            updater.triggerInstallation()
        }
    }
}
