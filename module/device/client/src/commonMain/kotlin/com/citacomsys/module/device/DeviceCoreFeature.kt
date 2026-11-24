package com.citacomsys.module.device

import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.module.device.di.deviceModule
import com.citacomsys.core.spi.FeatureNavigationPlugin
import io.github.aakira.napier.Napier
import org.koin.core.Koin

class DeviceCoreFeature : CoreFeature {
    override val id: String = "core.device"

    override val navigationPlugin: FeatureNavigationPlugin = DeviceNavigationPlugin()

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(deviceModule))
    }

    override fun deactivateService(koin: Koin) {
        Napier.d { "Device Core Feature Deactivated" }
        koin.unloadModules(listOf(deviceModule))
    }
}
