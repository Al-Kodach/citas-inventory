package com.citacomsys.api.device.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface DeviceNavKey : NavKey

@SerialName("device.management")
@Serializable
data object DeviceListNav : DeviceNavKey

@SerialName("device.summary")
@Serializable
data class WorkstationSummaryNav(val hardwareId: String) : DeviceNavKey

val deviceSerializerModule: SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(DeviceListNav::class, DeviceListNav.serializer())
        subclass(WorkstationSummaryNav::class, WorkstationSummaryNav.serializer())
    }
}
