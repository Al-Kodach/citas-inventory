package com.citacomsys.module.device.ui.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.device.service.DeviceApiService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.UserDeviceInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkstationListViewModel(
    private val apiService: DeviceApiService,
    private val store: PlatformRuntimeStore,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    val widgetDeviceList = store.orgDevices.asUiResult()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiResult.Loading)


    // Used by list screen, this ensures that we do not load devices for non admin dashboard users
    val devices: StateFlow<UiResult<List<UserDeviceInfo>>> = store.orgDevices
        .map { list ->
            if (list.isNotEmpty()) UiResult.Success(list) else UiResult.Loading
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = if (store.getOrgDevices()
                    .isNotEmpty()
            ) UiResult.Success(store.getOrgDevices()) else UiResult.Loading
        )

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            runCatching { apiService.getDevices() }
                .onSuccess { remoteDevices ->
                    store.saveOrgDevices(remoteDevices)
                }
                .onFailure { error ->
                    if (store.getOrgDevices().isEmpty()) {
                        showError(error.message ?: "Failed to fetch organization devices")
                    }
                }
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        viewModelScope.launch {
            showInfo("Renaming device...")
            apiService.renameDevice(deviceId, newName)
                .onSuccess {
                    showSuccess("Device renamed successfully")
                    loadDevices()
                }
                .onFailure {
                    showError("Failed to rename device: ${it.message}")
                }
        }
    }
}
