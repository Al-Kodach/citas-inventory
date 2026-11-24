package com.citacomsys.core.logging

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.LogMetadata
import com.citacomsys.feature.api.telemetry.telemetryContext
import com.citacomsys.core.domain.result.SystemActors
import kotlinx.coroutines.currentCoroutineContext
import kotlin.time.Clock

suspend fun buildMetaLog(
    level: TelemetryLevel,
    organizationId: String? = null,
    workspaceId: String? = null,
    deviceId: String? = null,
    branchId: String? = null,
): LogMetadata {
    val context = currentCoroutineContext().telemetryContext

    return LogMetadata(
        requestId = context?.requestId ?: "SYSTEM",
        orgId = organizationId ?: context?.organizationId ?: SystemActors.SYSTEM_ID,
        workspaceId = workspaceId ?: context?.workspaceId,
        branchId = branchId ?: context?.branchId,
        deviceId = deviceId ?: context?.deviceId,
        appFlavor = context?.appFlavor ?: "system_core",
        level = level,
        createdAt = Clock.System.now()
    )
}
