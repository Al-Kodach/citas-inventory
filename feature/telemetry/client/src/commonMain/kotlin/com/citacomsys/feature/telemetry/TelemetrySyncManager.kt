package com.citacomsys.feature.telemetry

import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TelemetrySyncManager(
    private val telemetryLogClient: TelemetryLogClient
) {
    private val syncMutex = Mutex()

    /**
     * Call this explicitly on-demand from ViewModel,
     * Composable lifecycle, or platform-native hooks.
     */
    suspend fun triggerOnDemandSync(): Result<Boolean> {
        // Prevent concurrent sync attempts if one is already running
        if (syncMutex.isLocked) {
            return Result.failure(IllegalStateException("Sync is already in progress"))
        }

        return syncMutex.withLock {
            try {
                val success = telemetryLogClient.syncLogsToServer().getOrThrow()
                Result.success(success)
            } catch (e: Exception) {
                // If network fails, logs safely remain inside the client's internal memory buffers
                Result.failure(e)
            }
        }
    }
}
