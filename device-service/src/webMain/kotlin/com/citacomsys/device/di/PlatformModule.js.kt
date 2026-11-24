package com.citacomsys.device.di

import com.citacomsys.device.connection.DeviceServiceConnection
import com.citacomsys.device.default.DefaultBiometricService
import com.citacomsys.device.default.DefaultDeviceLockService
import com.citacomsys.device.default.DefaultServiceConnection
import com.citacomsys.device.service.BiometricService
import com.citacomsys.device.service.DeviceLockService
import com.citacomsys.device.service.DeviceService
import com.citacomsys.device.service.JsService
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module {
    return module {
        single<DeviceService> { JsService(get()) }
        single<DeviceServiceConnection> { DefaultServiceConnection() }
        single<BiometricService> { DefaultBiometricService() }
        single<DeviceLockService> { DefaultDeviceLockService() }
    }
}