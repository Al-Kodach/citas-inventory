package com.citacomsys.device.default

import com.citacomsys.device.service.DeviceLockService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DefaultDeviceLockService : DeviceLockService {
    private val _locked = MutableStateFlow(false)
    override val isLocked: StateFlow<Boolean> = _locked.asStateFlow()

    override fun lock() {
        _locked.value = true
    }

    override fun unlock() {
        _locked.value = false
    }
}