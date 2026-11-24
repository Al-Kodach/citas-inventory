package com.citacomsys.core.logging

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.NetworkLog
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.telemetryContext
import kotlinx.coroutines.currentCoroutineContext

/**
 * High-Level API to log Network / Security mutations easily.
 */
suspend fun TelemetryAuditor.logNetworkSecurityEvent(
    triggerEvent: String,
    blockedDurationSeconds: Int? = null,
    organizationId: String? = null,
    workspaceId: String? = null,
    deviceId: String? = null,
    level: TelemetryLevel = TelemetryLevel.INFO,
): Result<Boolean> {

    val context = currentCoroutineContext().telemetryContext
    val metadata = buildMetaLog(
        level = level, 
        organizationId = organizationId, 
        workspaceId = workspaceId,
        deviceId = deviceId
    )

    return this.logNetworkSecurity(
        NetworkLog(
            metadata = metadata,
            clientIp = context?.clientIp ?: "Unknown Ip",
            endpointRoute = context?.endpointRoute ?: "Unknown Route",
            triggerEvent = triggerEvent,
            blockedDurationSeconds = blockedDurationSeconds,
        )
    )
}
