package com.citacomsys.core.mesh.di

import com.citacomsys.core.mesh.service.AndroidDiscoveryService
import com.citacomsys.core.mesh.service.AndroidLocalHubAdvertiser
import com.citacomsys.core.mesh.service.DiscoveryService
import com.citacomsys.core.mesh.service.LocalHubAdvertiser
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

actual fun meshPlatformModule() = module {
    single<DiscoveryService> { AndroidDiscoveryService(androidContext()) }
    single<LocalHubAdvertiser> { AndroidLocalHubAdvertiser(androidContext(), get()) }
}
