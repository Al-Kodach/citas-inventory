package com.citacomsys.inventory.admin.app

import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.sync.telemetry.TelemetryBackgroundScheduler
import com.citacomsys.core.sync.telemetry.TelemetrySyncConfig
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.feature.api.telemetry.dao.LocalTelemetryDao
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.Koin
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

internal class AdminTelemetryBackgroundScheduler(
    private val telemetryClient: TelemetryLogClient,
    private val connectivityService: NetworkConnectivityService,
    private val telemetryDao: LocalTelemetryDao,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : TelemetryBackgroundScheduler {

    private var workerJob: Job? = null
    override val id: String = "telemetry-background-worker"
    override val runLevel: StartupRunLevel
        get() = StartupRunLevel.POST_ONBOARDING

    /**
     * Initializes the continuous background polling daemon.
     * Usually invoked right inside your Main Application initialization lifecycle thread hook.
     */
    @OptIn(ExperimentalTime::class)
    override suspend fun run(koin: Koin) {
        if (workerJob != null) return // Already running

        Napier.i { "Initializing Multiplatform Telemetry Background Worker Daemon..." }

        workerJob = externalScope.launch {
            // Keep trackers for rolling time delta evaluation gates
            var lastIdentitySyncTime = Clock.System.now()
            var lastAppSyncTime = Clock.System.now()

            while (isActive) {
                // Short sleep cycle step to prevent heavy CPU spin cycles
                delay(10.seconds)


                // GATE 2: Wait until a secure network channel is actively available on the device
                if (!connectivityService.connectionStatusFlow.first()) {
                    continue
                }

                val now = Clock.System.now()
                // --- PHASE 1: EXECUTE URGENT IDENTITY/AUTHENTICATION LOG SYNC ---
                if (now - lastIdentitySyncTime >= TelemetrySyncConfig.IDENTITY_SYNC_INTERVAL) {
                    Napier.d { "Background worker waking up to process Identity Access log queue..." }

                    val result = telemetryClient.syncLogsToServer(
                        targetSources = setOf("IDENTITY"),
                        maxAgeWindow = now,
                        chunkLimit = TelemetrySyncConfig.BATCH_CHUNK_LIMIT
                    )

                    result.onSuccess {
                        lastIdentitySyncTime = now
                    }.onFailure { error ->
                        Napier.e { "Background Identity log sweep failed: ${error.message}" }
                    }
                }

                // --- PHASE 2: EXECUTE STANDARD APPLICATION MUTATION LOG SYNC ---
                if (now - lastAppSyncTime >= TelemetrySyncConfig.APPLICATION_SYNC_INTERVAL) {
                    Napier.d { "Background worker waking up to process Application Audit change ledger chunks..." }

                    val result = telemetryClient.syncLogsToServer(
                        targetSources = setOf("APPLICATION"),
                        maxAgeWindow = now,
                        chunkLimit = TelemetrySyncConfig.BATCH_CHUNK_LIMIT
                    )

                    result.onSuccess {
                        lastAppSyncTime = now
                    }.onFailure { error ->
                        Napier.e { "Background Application mutation log sweep failed: ${error.message}" }
                    }
                }

                // --- PHASE 3: SCHEDULED HARDWARE CACHE LEAN CLEANER ---
                // Prune old entries automatically every hour to maintain disk health caps
                if (now.toEpochMilliseconds() % 3600000 < 10000) {
                    runCatching {
                        telemetryDao.pruneExcessLogs(retentionCap = 5000L)
                        Napier.i { "Automated SQLite storage health cap pruning run completed successfully." }
                    }
                }
            }
        }
    }

    /**
     * Safely halts background threads when user logs out or app terminates.
     */
    private fun stopScheduledWorkerLoop() {
        Napier.i { "Halting Telemetry Background Sync Worker process threads..." }
        workerJob?.cancel()
        workerJob = null
    }
}
