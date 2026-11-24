package com.citacomsys.device.service

import com.citacomsys.device.service.DeviceLockService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class AndroidDeviceLockService : DeviceLockService {

    private val _locked = MutableStateFlow(false)
    override val isLocked: StateFlow<Boolean> = _locked

    override fun lock() {
        _locked.value = true
    }

    override fun unlock() {
        _locked.value = false
    }
}
