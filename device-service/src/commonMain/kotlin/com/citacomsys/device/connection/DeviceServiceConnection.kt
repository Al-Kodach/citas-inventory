package com.citacomsys.device.connection

import kotlinx.coroutines.flow.StateFlow

interface DeviceServiceConnection {
    val connectionState: StateFlow<DeviceServiceConnectionState>

    fun bind()

    fun unbind()
}