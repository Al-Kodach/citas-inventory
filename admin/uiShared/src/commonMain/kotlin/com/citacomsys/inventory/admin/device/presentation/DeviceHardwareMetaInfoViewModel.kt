package com.citacomsys.inventory.admin.device.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.device.AdminDeviceApiService
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.types.HardwareTechInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceHardwareMetaInfoViewModel(
    private val api: AdminDeviceApiService
): ViewModel() {

    private val _hardwareInfoState = MutableStateFlow<UiResult<HardwareTechInfo>>(UiResult.Loading)
    val deviceState: StateFlow<UiResult<HardwareTechInfo>> = _hardwareInfoState.asStateFlow()

    fun loadDeviceHardwareInfo(
        deviceId: String,
        device: HardwareTechInfo? = null
    ) {
        viewModelScope.launch {

            if (device != null) {
                _hardwareInfoState.value = UiResult.Success(device)
                return@launch
            }

            _hardwareInfoState.value = UiResult.Loading
            _hardwareInfoState.value = runCatching {
                api.getDevice(deviceId)
            }.asUiResult()
        }
    }
}
