package com.citacomsys.core.logging

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.IdentityLog
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.telemetryContext
import kotlinx.coroutines.currentCoroutineContext

suspend fun TelemetryAuditor.logIdentityAccessEvent(
    userId: String? = null,
    action: String, // LOGIN, LOGOUT, REFRESH_TOKEN
    outcome: String, // SUCCESS, FAILURE
    failureReason: String? = null,
    level: TelemetryLevel = TelemetryLevel.INFO,
): Result<Boolean> {
    val teleContext = currentCoroutineContext().telemetryContext
    val metadata = buildMetaLog(
        level,
        workspaceId = teleContext?.workspaceId,
        organizationId = teleContext?.organizationId,
        deviceId = teleContext?.deviceId
    )
    val userId = userId ?: teleContext?.userId ?: "SYSTEM_OPERATOR"

    return this.logIdentity(
        IdentityLog(
            metadata = metadata,
            userId = userId,
            action = action,
            outcome = outcome,
            failureReason = failureReason
        )
    )
}
