package com.citacomsys.kernel.server.di

import com.citacomsys.kernel.server.PlatformRuntimeService
import com.citacomsys.kernel.server.gateway.kernelRoute
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val kernelServerModule = module {
    singleOf(::PlatformRuntimeService)
}

fun Application.configureKernelServer() {
    loadKoinModules(kernelServerModule)

    routing {
        kernelRoute()
    }
}
