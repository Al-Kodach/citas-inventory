package com.citacomsys.core.security

import com.citacomsys.core.security.plugin.SecurityContextKey
import com.citacomsys.feature.api.telemetry.TelemetryRequestContext
import com.citacomsys.core.security.token.jwt.ActionPrincipal
import com.citacomsys.core.security.token.jwt.AdminDevicePrincipal
import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.core.security.token.jwt.WorkstationDevicePrincipal
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import io.ktor.util.generateNonceBlocking

val AuditContextKey = AttributeKey<TelemetryRequestContext>("AuditContext")

fun ApplicationCall.toAuditRequestContext(): TelemetryRequestContext {
    attributes.getOrNull(AuditContextKey)?.let { return it }
    val securityContext = attributes.getOrNull(SecurityContextKey)

    val userPrincipal = principal<UserPrincipal>()
    val actionPrincipal = principal<ActionPrincipal>()

    val derivedUserId = securityContext?.actorId ?: userPrincipal?.userId ?: actionPrincipal?.userId
    val derivedDeviceId = securityContext?.hardwareId ?: userPrincipal?.deviceId ?: request.headers["X-Hardware-Id"]
    val sessionId = securityContext?.sessionId ?: userPrincipal?.sessionId ?: actionPrincipal?.sessionId
    val trackingId = request.headers["X-Request-ID"] ?: generateNonceBlocking()
    val workspaceId = securityContext?.workspaceId ?: request.headers["X-Workspace-Id"]
    val branchId = request.headers["X-Branch-Id"] ?: request.headers["X-Branch-Id"]

    val context = TelemetryRequestContext(
        requestId = trackingId,
        clientIp = request.origin.remoteHost,
        endpointRoute = request.path(),
        appFlavor = request.headers["X-App-Flavor"] ?: "unknown_client",
        userAgent = request.headers["User-Agent"] ?: "unknown_agent",
        organizationId = userPrincipal?.organizationId,
        workspaceId = workspaceId,
        userId = derivedUserId,
        deviceId = derivedDeviceId,
        sessionId = sessionId,
        branchId = branchId,
    )

    attributes.put(AuditContextKey, context)
    return context
}
