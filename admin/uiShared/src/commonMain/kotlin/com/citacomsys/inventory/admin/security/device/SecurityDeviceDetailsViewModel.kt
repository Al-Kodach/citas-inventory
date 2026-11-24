package com.citacomsys.inventory.admin.security.device

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.device.AdminDeviceApiService
import com.citacomsys.feature.api.telemetry.SiemApiService
import com.citacomsys.feature.api.telemetry.TraceTimelineEntry
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.inventory.admin.common.ui.AdminBaseDetailsViewModel
import com.citacomsys.types.ActionConfirmationPurpose
import com.citacomsys.types.HardwareTechInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Administrative ViewModel for managing a specific device's details and lifecycle.
 *
 * Provides specialized operations like 'Rename' and 'Revoke' which are protected
 * by the Sudo Authentication pipeline.
 */
class SecurityDeviceDetailsViewModel(
    private val api: AdminDeviceApiService,
    private val siemApi: SiemApiService,
    notifier: InAppNotificationService,
    sudoAuthHandler: com.citacomsys.core.security.sudo.ActorSudoAuthHandler
) : AdminBaseDetailsViewModel<HardwareTechInfo>(notifier, sudoAuthHandler) {
    private val _endpointEvents = MutableStateFlow<UiResult<List<TraceTimelineEntry>>>(UiResult.Loading)
    val endpointEvents = _endpointEvents.asStateFlow()

    override suspend fun getDetails(itemId: String): HardwareTechInfo {
        Napier.d { "Loading Device info and endpoint events for device(id:$itemId)" }
        fetchEvents(itemId)
        return api.getDevice(itemId)
    }

    private fun fetchEvents(deviceId: String) {
        viewModelScope.launch {
            _endpointEvents.value = UiResult.Loading
            _endpointEvents.value = runCatching {
                siemApi.getDeviceTimeline(deviceId)
            }.asUiResult()
        }
    }


    /**
     * Updates the administrative display id of the device.
     * Triggers a Sudo Auth challenge before execution.
     */
    fun renameDevice(deviceId: String, newName: String) {
        authenticateAndExecute(
            title = "Rename Device",
            purpose = ActionConfirmationPurpose.UPDATE_SECURITY_SETTINGS,
            action = { api.renameDevice(deviceId, newName) }
        ) { result: Result<Unit> ->
            result.onSuccess {
                showSuccess("Device renamed successfully")
                loadDetails(deviceId) // Refresh local state
            }.onFailure {
                showError(it.message ?: "Failed to rename device")
            }
        }
    }

    /**
     * Revokes the device's authorization (locks the device).
     * High-stakes operation protected by a specific Sudo purpose.
     */
    fun revokeDevice(deviceId: String, onRevoked: () -> Unit) {
        authenticateAndExecute(
            title = "Revoke Device Access",
            purpose = ActionConfirmationPurpose.DELETE_RESOURCES,
            action = { api.revokeDevice(deviceId) }
        ) { result: Result<Unit> ->
            result.onSuccess {
                showSuccess("Device access revoked")
                onRevoked()
            }.onFailure {
                showError(it.message ?: "Failed to revoke device")
            }
        }
    }
}