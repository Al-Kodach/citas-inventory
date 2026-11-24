package com.citacomsys.device.di

import com.citacomsys.device.service.AndroidBiometricService
import com.citacomsys.device.service.AndroidDeviceLockService
import com.citacomsys.device.service.AndroidDeviceService
import com.citacomsys.device.service.BiometricService
import com.citacomsys.device.service.DeviceLockService
import com.citacomsys.device.service.DeviceService
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module {
    return module {
        single<DeviceService> {
            AndroidDeviceService(
                context = get(),
                ksafe = get()
            )
        }

        single<BiometricService> { AndroidBiometricService(get()) }
        single<DeviceLockService> { AndroidDeviceLockService() }
    }
}
