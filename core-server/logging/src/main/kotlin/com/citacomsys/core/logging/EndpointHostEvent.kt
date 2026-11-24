package com.citacomsys.core.logging

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.EndpointHostLog
import com.citacomsys.feature.api.telemetry.SecurityActionEvent
import com.citacomsys.feature.api.telemetry.TelemetryAuditor

suspend fun TelemetryAuditor.logEndpointHostEvent(
    organizationId: String? = null,
    workspaceId: String? = null,
    deviceId: String? = null,
    osName: String,
    architecture: String,
    action: SecurityActionEvent, 
    fingerprintHash: String,
    level: TelemetryLevel = TelemetryLevel.INFO
): Result<Boolean> {
    val metadata = buildMetaLog(
        level = level,
        workspaceId = workspaceId,
        organizationId = organizationId,
        deviceId = deviceId
    )

    val event = EndpointHostLog(
        metadata = metadata,
        osName = osName,
        architecture = architecture,
        action = action,
        fingerprintHash = fingerprintHash
    )

    return this.logEndpoint(event)
}
