package com.citacomsys.device.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DesktopDeviceLockService : DeviceLockService {
    private val _locked = MutableStateFlow(false)
    override val isLocked: StateFlow<Boolean> = _locked.asStateFlow()

    override fun lock() {
        _locked.value = true
    }

    override fun unlock() {
        _locked.value = false
    }
}