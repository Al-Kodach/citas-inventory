package com.citacomsys.module.device.ui.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.auth.WorkstationSession
import com.citacomsys.api.device.service.DeviceApiService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.types.DeviceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkstationSummaryViewModel(
    hardwareId: String,
    private val apiService: DeviceApiService,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {
    private val _deviceSummaryState = MutableStateFlow<UiResult<DeviceSummary>>(UiResult.Idle)
    val deviceSummaryState = _deviceSummaryState.asStateFlow()


    init {
        loadDeviceSummary(hardwareId)
    }

    fun loadDeviceSummary(hardwareId: String) {
        if (_deviceSummaryState.value == UiResult.Loading) return
        viewModelScope.launch {
            _deviceSummaryState.value = UiResult.Loading
            _deviceSummaryState.value =
                runCatching { apiService.getDeviceSummary(hardwareId) }.asUiResult()
        }
    }


    fun renameDevice(deviceId: String, newName: String) {
        viewModelScope.launch {
            showInfo("Renaming device...")
            apiService.renameDevice(deviceId, newName)
                .onSuccess {
                    showSuccess("Device renamed successfully")
                    loadDeviceSummary(deviceId)
                }
                .onFailure { showError("Failed to rename device") }
        }
    }

    fun unbindWorkstation(bindingId: String, hardwareId: String) {
        viewModelScope.launch {
            showInfo("Unbinding store access...")
            apiService.unbindWorkstation(bindingId)
                .onSuccess {
                    showSuccess("Store access unlinked successfully")
                    loadDeviceSummary(hardwareId)
                }
                .onFailure { showError(it.message ?: "Failed to unlink store access") }
        }
    }

    fun relinkWorkstation(bindingId: String, hardwareId: String) {
        viewModelScope.launch {
            showInfo("Relinking store access...")
            apiService.relinkWorkstation(bindingId)
                .onSuccess {
                    showSuccess("Store access relinked successfully")
                    loadDeviceSummary(hardwareId)
                }
                .onFailure {
                    showError(
                        it.message
                            ?: "Failed to relink store access. Ensure store has device capacity slots."
                    )
                }
        }
    }

    fun revokeDeviceGlobally(hardwareId: String) {
        viewModelScope.launch {
            showInfo("Revoking device globally...")
            apiService.revokeDeviceGlobally(hardwareId)
                .onSuccess {
                    showSuccess("Device revoked globally")
                    loadDeviceSummary(hardwareId)
                }
                .onFailure { showError(it.message ?: "Failed to revoke device globally") }
        }
    }

    fun revokeSession(sessionId: String, hardwareId: String) {
        viewModelScope.launch {
            showInfo("Terminating active session...")
            apiService.revokeSession(sessionId)
                .onSuccess {
                    showSuccess("Session terminated successfully")
                }
                .onFailure { showError(it.message ?: "Failed to terminate session") }
        }
    }
}
