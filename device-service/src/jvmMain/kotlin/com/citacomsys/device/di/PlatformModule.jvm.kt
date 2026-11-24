package com.citacomsys.device.di

import com.citacomsys.device.service.BiometricService
import com.citacomsys.device.service.DesktopBiometricService
import com.citacomsys.device.service.DesktopDeviceLockService
import com.citacomsys.device.service.DesktopDeviceService
import com.citacomsys.device.service.DeviceLockService
import com.citacomsys.device.service.DeviceService
import com.citacomsys.device.connection.DesktopServiceConnection
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module {
    return module {
        single<DeviceService> {
            DesktopDeviceService(
                appConfig = get(),
                ksafe = get()
            )
        }
        single { DesktopServiceConnection() }
        single<BiometricService> { DesktopBiometricService() }
        single<DeviceLockService> { DesktopDeviceLockService() }
    }
}
