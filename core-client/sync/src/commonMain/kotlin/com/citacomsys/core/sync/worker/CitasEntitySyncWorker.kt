package com.citacomsys.core.sync.worker

import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.spi.NetworkConstraint
import com.citacomsys.core.spi.PlatformWorker
import com.citacomsys.core.spi.WorkerConstraints
import com.citacomsys.core.spi.WorkerPolicy
import com.citacomsys.core.spi.WorkerResult
import com.citacomsys.core.spi.WorkerScope
import com.citacomsys.core.sync.engine.EntitySyncEngine
import io.github.aakira.napier.Napier
import kotlin.time.Duration.Companion.minutes

class CitasEntitySyncWorker(
    private val syncPrefs: SyncPreferencesRepository,
    private val syncEngine: EntitySyncEngine,
) : PlatformWorker {

    override val workerId: String = "citas_entity_sync_worker"
    override val workerScope: WorkerScope = WorkerScope.GLOBAL

    override val policy: WorkerPolicy
        get() {
            val config = syncPrefs.syncConfig.value
            val interval = config.syncIntervalMinutes.coerceAtLeast(1).minutes
            return WorkerPolicy.Periodic(interval = interval)
        }

    override val constraints: WorkerConstraints
        get() {
            val config = syncPrefs.syncConfig.value
            return WorkerConstraints(
                network = if (config.allowCellularSync) NetworkConstraint.CONNECTED else NetworkConstraint.UNMETERED,
                requiresCharging = config.requiresCharging,
                requiresDeviceIdle = config.requiresDeviceIdle,
            )
        }

    override suspend fun doWork(workspaceId: String?): WorkerResult {
        return try {
            val config = syncPrefs.syncConfig.value
            Napier.i(tag = "CITAS_SYNC_WORKER") { "Executing scheduled sync cycle (Targets: ${config.targets.size}, Workspace: ${workspaceId ?: "ALL"})..." }
            syncEngine.syncAll()
            WorkerResult.SUCCESS
        } catch (e: Exception) {
            Napier.e(tag = "CITAS_SYNC_WORKER", throwable = e) { "SyncWorker failed to complete sync cycle" }
            WorkerResult.RETRY
        }
    }
}
