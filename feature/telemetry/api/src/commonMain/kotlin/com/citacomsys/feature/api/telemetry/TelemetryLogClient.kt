package com.citacomsys.feature.api.telemetry

import io.github.aakira.napier.LogLevel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface TelemetryLogClient {

    /**
     * Track user authentications or failures locally on the device.
     */
    suspend fun trackIdentityEvent(
        userId: String,
        action: String,
        outcome: String,
        reason: String? = null,
        workspaceId: String? = null,
        branchId: String? = null
    )

    /**
     * Track system manipulation, configuration updates, or screen flows inside the apps.
     */
    suspend fun trackApplicationEvent(
        actorId: String,
        entityType: String,
        entityId: String,
        action: String,
        summary: String,
        workspaceId: String? = null,
        branchId: String? = null
    )

    fun log(
        level: LogLevel,
        message: String,
        action: String,
        workspaceId: String? = null,
        branchId: String? = null
    )

    @OptIn(ExperimentalTime::class)
    suspend fun syncLogsToServer(
        targetSources: Set<String> = emptySet(),
        maxAgeWindow: Instant? = null,
        chunkLimit: Long = 100L
    ): Result<Boolean>
}
