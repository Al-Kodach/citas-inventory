package com.citacomsys.device.default

import com.citacomsys.device.connection.DeviceServiceConnection
import com.citacomsys.device.connection.DeviceServiceConnectionState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DefaultServiceConnection : DeviceServiceConnection {
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