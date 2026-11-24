package com.citacomsys.core.logging

import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.feature.api.telemetry.ApplicationLog
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.OperationalLog
import com.citacomsys.feature.api.telemetry.telemetryContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.json.JsonElement

/**
 * High-Level API to log Business Logic / App mutations easily.
 */
suspend fun TelemetryAuditor.logApplicationEvent(
    action: String,
    entityType: String,
    entityId: String,
    summary: String,
    level: TelemetryLevel = TelemetryLevel.INFO,
    payloadChanges: JsonElement? = null,
    actorIdOverride: String? = null,
    organizationIdOverride: String? = null,
    workspaceIdOverride: String? = null,
    deviceIdOverride: String? = null,
): Result<Boolean> {

    val context = currentCoroutineContext().telemetryContext

    val metadata = buildMetaLog(
        level = level,
        organizationId = organizationIdOverride,
        workspaceId = workspaceIdOverride,
        deviceId = deviceIdOverride
    )

    return this.logApplication(
        ApplicationLog(
            metadata = metadata,
            actorId = actorIdOverride ?: context?.userId ?: SystemActors.SYSTEM_USER_ID.toString(),
            entityType = entityType,
            entityId = entityId,
            action = action,
            summary = summary,
            payloadChanges = payloadChanges
        )
    )
}


suspend fun TelemetryAuditor.logOperationalEvent(
    component: String,
    messagge: String,
    level: TelemetryLevel = TelemetryLevel.INFO,
): Result<Boolean> {
    val metadata = buildMetaLog(level = level)
    return logOperational(
        OperationalLog(
            metadata = metadata,
            component = component,
            message = messagge
        )
    )
}
