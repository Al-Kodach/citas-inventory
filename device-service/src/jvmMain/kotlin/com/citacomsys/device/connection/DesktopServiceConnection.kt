package com.citacomsys.device.connection

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DesktopServiceConnection : DeviceServiceConnection {
    private val _deviceServiceConnectionState =
        MutableStateFlow(DeviceServiceConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<DeviceServiceConnectionState> =
        _deviceServiceConnectionState.asStateFlow()

    override fun bind() {
        Napier.d { "DesktopServiceConnection.bind()" }
        _deviceServiceConnectionState.value = DeviceServiceConnectionState.CONNECTED
    }

    override fun unbind() {
        Napier.d { "DesktopServiceConnection.unbind()" }
        _deviceServiceConnectionState.value = DeviceServiceConnectionState.DISCONNECTED
    }
}