package com.citacomsys.core.logging

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.OperationalLog
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import java.io.PrintWriter
import java.io.StringWriter

suspend fun TelemetryAuditor.logException(
    component: String,
    exception: Throwable,
    executionTime: Long? = null,
    organizationId: String? = null,
    workspaceId: String? = null,
    deviceId: String? = null,
) {
    val sw = StringWriter()
    exception.printStackTrace(PrintWriter(sw))

    val metadata = buildMetaLog(
        level = TelemetryLevel.ERROR, 
        organizationId = organizationId, 
        workspaceId = workspaceId,
        deviceId = deviceId
    )

    this.logOperational(
        OperationalLog(
            metadata = metadata,
            component = component,
            message = exception.message ?: "Unknown runtime error occurred",
            executionTimeMs = executionTime,
            stack_trace = sw.toString()
        )
    )
}
