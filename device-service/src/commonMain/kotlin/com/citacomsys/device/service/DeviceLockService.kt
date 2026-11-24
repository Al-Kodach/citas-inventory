package com.citacomsys.device.service

import kotlinx.coroutines.flow.StateFlow

interface DeviceLockService {

    fun lock()

    fun unlock()

    val isLocked: StateFlow<Boolean>
}