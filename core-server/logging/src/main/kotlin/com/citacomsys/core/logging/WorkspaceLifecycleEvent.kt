package com.citacomsys.core.logging

import com.citacomsys.feature.api.telemetry.ApplicationLog
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.telemetryContext
import com.citacomsys.core.domain.result.SystemActors
import kotlinx.coroutines.currentCoroutineContext

/**
 * High-Level API to log explicit Workspace/Organization Lifecycle events.
 */
suspend fun TelemetryAuditor.logTenantLifecycleEvent(
    targetId: String,
    action: String, // e.g., "WORKSPACE_PROVISIONED", "WORKSPACE_SUSPENDED"
    summary: String,
    level: TelemetryLevel = TelemetryLevel.INFO
): Result<Boolean> {

    val context = currentCoroutineContext().telemetryContext

    val metadata = buildMetaLog(level, workspaceId = targetId)

    return this.logApplication(
        ApplicationLog(
            metadata = metadata,
            actorId = context?.userId ?: SystemActors.SYSTEM_USER_ID.toString(),
            entityType = "WORKSPACE_LIFECYCLE",
            entityId = targetId,
            action = action,
            summary = summary,
            payloadChanges = null
        )
    )
}
