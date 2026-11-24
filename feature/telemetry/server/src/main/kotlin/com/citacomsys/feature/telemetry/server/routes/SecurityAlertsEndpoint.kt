package com.citacomsys.feature.telemetry.server.routes

import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.feature.api.telemetry.*
import com.citacomsys.core.security.toAuditRequestContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlin.time.Clock


internal fun Route.configureSecurityAlertsEndpoint(
    auditor: TelemetryAuditor,
    alertService: SecurityAlertService
) {
    authenticate("device-access-auth") {
        post("/bypass-alert") {
            val payload = call.receive<SecurityBypassPayload>()
            val requestContext = call.toAuditRequestContext()

            val organizationId = call.resolveOrganizationId
            val workspaceId = call.resolvedWorkspaceId
            val managerId = payload.managerUserId

            auditor.logIdentity(
                IdentityLog(
                    metadata = LogMetadata(
                        requestId = requestContext.requestId,
                        orgId = organizationId,
                        workspaceId = workspaceId,
                        deviceId = requestContext.deviceId,
                        appFlavor = requestContext.appFlavor,
                        level = TelemetryLevel.CRITICAL,
                        createdAt = Clock.System.now()
                    ),
                    userId = managerId,
                    action = "MANDATORY_UPDATE_BYPASS",
                    outcome = "OVERRIDE_GRANTED",
                    failureReason = "Manager authorized local bypass. Local: ${payload.currentDeviceVersion} < Target: ${payload.targetedForcedVersion}"
                )
            )

            alertService.dispatchImmediateSecurityAlert(
                organizationId = organizationId,
                workspaceId = workspaceId,
                action = "MANDATORY_UPDATE_BYPASS",
                summary = "User $managerId authorized a version override (Local: ${payload.currentDeviceVersion})"
            )

            call.respond(HttpStatusCode.Accepted)
        }
    }
}
