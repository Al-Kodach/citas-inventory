package com.citacomsys.core.sync.telemetry

import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.feature.api.telemetry.dao.LocalTelemetryDao
import com.citacomsys.types.ProductModule
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

internal class DefaultTelemetryBackgroundScheduler(
    private val telemetryClient: TelemetryLogClient,
    private val connectivityService: NetworkConnectivityService,
    private val sessionManager: AppSessionManager,
    private val workspaceService: PlatformClientService,
    private val telemetryDao: LocalTelemetryDao,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : TelemetryBackgroundScheduler {

    private var workerJob: Job? = null

    override val id: String = "core.telemetry.sync"
    override val runLevel: StartupRunLevel = StartupRunLevel.POST_ONBOARDING

    private fun stopScheduledWorkerLoop() {
        Napier.i { "Halting Telemetry Background Sync Worker process threads..." }
        workerJob?.cancel()
        workerJob = null
    }

    override suspend fun run(koin: Koin) {
        if (workerJob != null) return // Already running

        Napier.i { "Initializing Multiplatform Telemetry Background Worker Daemon..." }

        workerJob = externalScope.launch {
            var lastIdentitySyncTime = Clock.System.now()
            var lastAppSyncTime = Clock.System.now()

            while (isActive) {
                delay(10.seconds)

                // GATE 1: If the workspace is offline local / standalone (not cloud-synced or satellite), skip cloud sync
                if (!sessionManager.isSubscribed()) {
                    continue
                }

                // GATE 2: Wait until a secure network channel is actively available on the device
                if (!connectivityService.connectionStatusFlow.first()) {
                    continue
                }

                // GATE 3: Verify Enterprise Licensing
                val activeModules = workspaceService.modulesMap.value
                val isSiemLicensed = activeModules.containsKey(ProductModule.CROSS_BRANCH_SIEM_AUDIT)

                if (!isSiemLicensed) {
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

                // --- 🧹 PHASE 3: SCHEDULED HARDWARE CACHE LEAN CLEANER ---
                if (now.toEpochMilliseconds() % 3600000 < 10000) {
                    runCatching {
                        telemetryDao.pruneExcessLogs(retentionCap = 5000L)
                        Napier.i { "Automated SQLite storage health cap pruning run completed successfully." }
                    }
                }
            }
        }
    }
}

interface TelemetryBackgroundScheduler : StartupTask
