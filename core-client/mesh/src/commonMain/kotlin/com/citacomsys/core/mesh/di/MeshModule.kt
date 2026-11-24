package com.citacomsys.core.mesh.di

import com.citacomsys.core.domain.file.PlatformFileSystem
import com.citacomsys.core.mesh.server.LocalMeshServerEngine
import com.citacomsys.core.mesh.server.createMeshServerEngine
import com.citacomsys.core.mesh.service.*
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val meshModule = module {
    includes(meshPlatformModule())

    single<LocalMeshServerEngine> {
        createMeshServerEngine(
            gateways = getAll(),
            platformStore = get(),
            filesDir = get<PlatformFileSystem>().getInternalDir().toString()
        )
    }

    singleOf(::MeshActivityStore)
    singleOf(::SatelliteNodeManager)
    single {
        MotherHubManager(
            deviceService = get(),
            platformStore = get(),
            hubAdvertiser = get(),
            serverEngine = get(),
            prefs = get(),
            activityStore = get(),
            dbManager = get(),
            plugins = getAll(),
            scope = get()
        )
    }

    single<MeshManager> {
        MeshManagerImpl(
            satelliteNodeManager = get(),
            motherHubManager = get(),
            platformStore = get(),
            sessionManager = get(),
            prefs = get(),
            notificationService = get(),
            scope = get(),
        )
    }
}

expect fun meshPlatformModule(): Module
