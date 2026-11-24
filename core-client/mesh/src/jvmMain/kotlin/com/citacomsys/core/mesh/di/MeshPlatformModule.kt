package com.citacomsys.core.mesh.di

import com.citacomsys.core.mesh.service.DiscoveryService
import com.citacomsys.core.mesh.service.JvmDiscoveryService
import com.citacomsys.core.mesh.service.LocalHubAdvertiser
import com.citacomsys.core.mesh.service.JvmLocalHubAdvertiser
import org.koin.dsl.module

actual fun meshPlatformModule() = module {
    single<DiscoveryService> { JvmDiscoveryService() }
    single<LocalHubAdvertiser> { JvmLocalHubAdvertiser(get()) }
}
