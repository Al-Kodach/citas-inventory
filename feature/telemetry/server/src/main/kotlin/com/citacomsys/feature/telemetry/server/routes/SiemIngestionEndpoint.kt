package com.citacomsys.feature.telemetry.server.routes

import com.citacomsys.feature.api.telemetry.*
import com.citacomsys.feature.api.telemetry.model.ClientLogSyncBatch
import com.citacomsys.feature.api.telemetry.model.TelemetrySyncResponse
import com.citacomsys.core.security.toAuditRequestContext
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

internal fun Route.configureSiemIngestionEndpoint() {
    val auditor: TelemetryAuditor by inject()

    post("/api/v1/telemetry/sync") {
        val batch = call.receive<ClientLogSyncBatch>()
        val requestContext = call.toAuditRequestContext()

        val orgId = requestContext.organizationId ?: "CLIENT_UNKNOWN"
        val workspaceId = requestContext.workspaceId ?: "CLIENT_UNKNOWN"
        val deviceStr = batch.deviceId

        call.application.launch {
            batch.identityLogs.forEach { logDto ->
                val metadata = LogMetadata(
                    requestId = requestContext.requestId,
                    orgId = orgId,
                    workspaceId = workspaceId,
                    branchId = logDto.branchId,
                    deviceId = deviceStr,
                    appFlavor = batch.appFlavor,
                    level = TelemetryLevel.entries.find { it.name == logDto.level.uppercase() } ?: TelemetryLevel.INFO,
                    createdAt = logDto.createdAt
                )
                auditor.logIdentity(
                    IdentityLog(
                        metadata = metadata,
                        userId = logDto.userId,
                        action = logDto.action,
                        outcome = logDto.getOutcomeFromPayload(),
                        failureReason = logDto.getReasonFromPayload()
                    )
                )
            }

            batch.applicationLogs.forEach { logDto ->
                val metadata = LogMetadata(
                    requestId = requestContext.requestId,
                    orgId = orgId,
                    workspaceId = workspaceId,
                    branchId = logDto.branchId,
                    deviceId = deviceStr,
                    appFlavor = batch.appFlavor,
                    level = TelemetryLevel.entries.find { it.name == logDto.level.uppercase() } ?: TelemetryLevel.INFO,
                    createdAt = logDto.createdAt
                )
                auditor.logApplication(
                    ApplicationLog(
                        metadata = metadata,
                        actorId = logDto.userId ?: "UNKNOWN",
                        entityType = logDto.getEntityTypeFromPayload(),
                        entityId = logDto.getEntityIdFromPayload(),
                        action = logDto.action,
                        summary = logDto.getSummaryFromPayload()
                    )
                )
            }
        }

        call.respond(
            HttpStatusCode.OK,
            TelemetrySyncResponse(
                status = "synchronized",
                processedCount = batch.identityLogs.size + batch.applicationLogs.size
            )
        )
    }
}
