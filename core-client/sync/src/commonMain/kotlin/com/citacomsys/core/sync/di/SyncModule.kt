package com.citacomsys.core.sync.di

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.service.DataSyncTrigger
import com.citacomsys.core.mesh.gateway.BatchRelayGateway
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.spi.PlatformWorker
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.core.sync.engine.impl.BatchEntitySyncEngine
import com.citacomsys.core.sync.handler.DefaultOfflineSyncWorker
import com.citacomsys.core.sync.telemetry.DefaultTelemetryBackgroundScheduler
import com.citacomsys.core.sync.telemetry.TelemetryBackgroundScheduler
import com.citacomsys.core.sync.worker.CitasEntitySyncWorker
import com.citacomsys.device.service.DeviceService
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.time.Clock

val syncModule = module {
    single {
        BatchEntitySyncEngine(
            defaultWorker = get<DefaultOfflineSyncWorker>(),
            manager = get<InventoryDatabaseManager>(),
            telemetryClient = get<TelemetryLogClient>(),
            scope = get<CoroutineScope>(),
            clock = get<Clock>(),
            syncPrefs = get<SyncPreferencesRepository>(),
            deviceService = get<DeviceService>()
        )
    } binds arrayOf(EntitySyncEngine::class, DataSyncTrigger::class)

    singleOf(::CitasEntitySyncWorker) {
        bind<PlatformWorker>()
    }

    singleOf(::DefaultOfflineSyncWorker)
    singleOf(::BatchRelayGateway) {
        bind<HubServiceGateway>()
    }
    singleOf(::DefaultTelemetryBackgroundScheduler) {
        bind<TelemetryBackgroundScheduler>()
        bind<StartupTask>()
    }
}
