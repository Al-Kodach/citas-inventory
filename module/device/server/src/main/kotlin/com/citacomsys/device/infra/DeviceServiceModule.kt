package com.citacomsys.device.infra

import com.citacomsys.api.device.node.DeviceRepository
import com.citacomsys.device.infra.gateway.DeviceController
import com.citacomsys.device.infra.repository.ExposedDeviceRepository
import org.koin.dsl.module

val deviceServiceModule = module {
    // repository
    single<DeviceRepository> { ExposedDeviceRepository() }
    single { DeviceController(get(), get(), get(), get()) }
}