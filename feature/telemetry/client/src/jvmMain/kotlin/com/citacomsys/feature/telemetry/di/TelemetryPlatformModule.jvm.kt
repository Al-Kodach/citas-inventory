package com.citacomsys.feature.telemetry.di

import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import com.citacomsys.feature.telemetry.DesktopAppUpdater
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformTelemetryModule(): Module = module {
    singleOf(::DesktopAppUpdater) {
        bind<PlatformAppUpdater>()
    }
}
