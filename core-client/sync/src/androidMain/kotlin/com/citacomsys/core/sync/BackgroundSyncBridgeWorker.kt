package com.citacomsys.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.citacomsys.core.sync.engine.EntitySyncEngine
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes

class BackgroundSyncBridgeWorker(
    context: Context,
    params: WorkerParameters,
    private val syncEngine: EntitySyncEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Fire your engine's existing native commands mechanism
            // This wakes up your 'runFullSyncCycle()' naturally
            syncEngine.syncAll()

            // Await your engine's state machine to finish processing
            // Wait for 'isSyncing' to transition from true back to false
            withTimeoutOrNull(9.minutes) {
                // Wait until the engine reports it has entered an active sync state
                syncEngine.isSyncing.first { it }
                // Wait until the engine returns back to an idle state
                syncEngine.isSyncing.first { !it }
            }

            Result.success()
        } catch (e: Exception) {
            Napier.e(e, tag = "BackgroundSyncBridgeWorker") { "Sync worker error"}
            // If the process fails or timed out, WorkManager can safely queue a system retry
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
