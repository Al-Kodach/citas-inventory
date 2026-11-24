package com.citacomsys.kernel.client.di

import com.citacomsys.core.spi.PlatformWorker
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.kernel.client.KtorKernelApiService
import com.citacomsys.kernel.client.OrganizationConfigStateManager
import com.citacomsys.kernel.client.PlatformClientServiceImpl
import com.citacomsys.kernel.client.PlatformRuntimeStoreImpl
import com.citacomsys.kernel.client.RuntimeSecurityHandler
import com.citacomsys.kernel.client.WorkspaceConfigurationManager
import com.citacomsys.kernel.client.WorkspaceEnvironmentManager
import com.citacomsys.kernel.client.worker.OrgConfigSyncWorker
import com.citacomsys.kernel.client.worker.WorkspaceEnvironmentSyncWorker
import com.citacomsys.kernel.service.KernelApiService
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val kernelClientModule = module {
    single<PlatformRuntimeStore> {
        PlatformRuntimeStoreImpl(
            vault = get(named("vault")),
            prefs = get(),
            scope = get()
        )
    }

    single {
        RuntimeSecurityHandler(
            platformRuntimeStore = get(),
            tokenStore = get(),
            preferencesRepository = get(),
            offlineAuthenticator = get()
        )
    }

    single<KernelApiService> { KtorKernelApiService(get()) }
    singleOf(::OrganizationConfigStateManager) bind StartupTask::class
    singleOf(::WorkspaceEnvironmentManager)
    singleOf(::WorkspaceConfigurationManager)
    singleOf(::OrgConfigSyncWorker) bind PlatformWorker::class
    singleOf(::WorkspaceEnvironmentSyncWorker) bind PlatformWorker::class

    single<PlatformClientService> {
        PlatformClientServiceImpl(
            kernelApiService = get(),
            store = get(),
            sessionManager = get(),
            environmentManager = get(),
            orgConfigStateManager = get(),
            securityHandler = get(),
            systemEventService = get(),
            tokenStore = get(),
            preferencesRepository = get(),
            scope = get()
        )
    }
}
