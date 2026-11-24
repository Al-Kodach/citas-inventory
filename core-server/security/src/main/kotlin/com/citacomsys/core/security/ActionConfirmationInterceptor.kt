package com.citacomsys.core.security

import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.IdentityLog
import com.citacomsys.feature.api.telemetry.LogMetadata
import com.citacomsys.core.security.plugin.actionPrincipal
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.security.service.SecurityTrackableEvent
import com.citacomsys.core.security.token.jwt.ActionPrincipal
import com.citacomsys.types.ActionConfirmationPurpose
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlin.time.Clock


private val logger = mu.KotlinLogging.logger { }

suspend fun RoutingContext.withActionConfirmation(
    auditor: TelemetryAuditor,
    alertService: SecurityAlertService,
    purpose: ActionConfirmationPurpose,
    body: suspend (ActionPrincipal) -> Unit
) {
    logger.info { "Checking Action Confirmation Purpose..." }
    val principal = call.actionPrincipal()

    if (principal.action != purpose) {
        logger.warn { "Action Confirmation Purpose Mismatch: ${principal.action} != $purpose" }

        val auditContext = call.toAuditRequestContext()

        auditor.logIdentity(
            IdentityLog(
                metadata = LogMetadata(
                    requestId = auditContext.requestId,
                    orgId = principal.organizationId,
                    workspaceId = principal.workspaceId,
                    deviceId = auditContext.deviceId,
                    appFlavor = auditContext.appFlavor,
                    level = TelemetryLevel.CRITICAL,
                    createdAt = Clock.System.now()
                ),
                userId = principal.userId,
                action = "ACT_PURPOSE_MISMATCH",
                outcome = "FAILURE",
                failureReason = "Expected $purpose but found ${principal.action}"
            )
        )

        alertService.dispatchImmediateSecurityAlert(
            organizationId = principal.organizationId,
            workspaceId = principal.workspaceId,
            action = "ACTION_TOKEN_MISMATCH_LITE",
            summary = "User ${principal.userId} attempted $purpose but token is for ${principal.action}."
        )

        return call.respond(HttpStatusCode.Forbidden, "Invalid Action Purpose")
    }

    logger.info { "Action Confirmation Purpose Matched: ${principal.action} == $purpose" }
    body(principal)
}


suspend fun RoutingContext.withActionConfirmation(
    auditor: TelemetryAuditor,
    securityThrottler: SecurityThrottlerService,
    alertService: SecurityAlertService,
    purpose: ActionConfirmationPurpose,
    metaBlock: suspend (ActionPrincipal) -> Pair<String, String>, 
    body: suspend (ActionPrincipal) -> Unit
) {
    val logger = mu.KotlinLogging.logger("SECURITY_KERNEL_ACTION")

    val actionPrincipal = call.principal<ActionPrincipal>()

    val auditContext = call.toAuditRequestContext()

    val currentUserId = auditContext.userId ?: "ANONYMOUS_ACTOR"
    val currentOrgId = auditContext.organizationId ?: "GLOBAL_SYSTEM"
    val currentWorkspaceId = auditContext.workspaceId ?: "GLOBAL_SYSTEM"

    if (actionPrincipal == null) {
        logger.error { "CRITICAL THREAT: Attempted to bypass secure confirmation gate without presenting an ActionPrincipal token. Path: ${auditContext.endpointRoute}" }

        securityThrottler.attempt(
            key = currentUserId,
            event = SecurityTrackableEvent.MALICIOUS_CLAIM_MISMATCH,
            maxAttempts = 1
        )

        alertService.dispatchImmediateSecurityAlert(
            organizationId = currentOrgId,
            workspaceId = currentWorkspaceId,
            action = "UNAUTHORIZED_GATE_BYPASS_ATTEMPT",
            summary = "Attempted to bypass secure confirmation gate without ActionPrincipal token at ${auditContext.endpointRoute}."
        )

        call.respond(HttpStatusCode.Unauthorized, "Action Confirmation Token Required.")
        return
    }

    val (resourceName, auditSummary) = metaBlock(actionPrincipal)

    if (actionPrincipal.action != purpose) {
        logger.error { "SECURITY ALERT: Action Purpose Token Contamination. Operator [User: $currentUserId] presented token type '${actionPrincipal.action}' but target endpoint requires explicit claim purpose: '$purpose'." }

        securityThrottler.attempt(
            key = currentUserId,
            event = SecurityTrackableEvent.MALICIOUS_CLAIM_MISMATCH,
            maxAttempts = 1 
        )

        auditor.logIdentity(
            IdentityLog(
                metadata = LogMetadata(
                    requestId = auditContext.requestId,
                    orgId = actionPrincipal.organizationId,
                    workspaceId = actionPrincipal.workspaceId,
                    deviceId = auditContext.deviceId,
                    appFlavor = auditContext.appFlavor,
                    level = TelemetryLevel.CRITICAL,
                    createdAt = Clock.System.now()
                ),
                userId = currentUserId,
                action = "ACTION_CLAIM_VIOLATION",
                outcome = "DENIED",
                failureReason = "Token misaligned. Expected: $purpose, Found: ${actionPrincipal.action}. Target Resource: $resourceName"
            )
        )

        alertService.dispatchImmediateSecurityAlert(
            organizationId = actionPrincipal.organizationId,
            workspaceId = actionPrincipal.workspaceId,
            action = "ACTION_TOKEN_MISMATCH",
            summary = "User $currentUserId presented token type '${actionPrincipal.action}' but target endpoint requires '$purpose'. Target Resource: $resourceName"
        )

        call.respond(
            HttpStatusCode.Forbidden,
            "Invalid Security Token Purpose Claim Scope Context."
        )
        return
    }

    securityThrottler.attempt(
        key = currentUserId,
        event = SecurityTrackableEvent.STOCK_TRANSFER_ATTEMPT,
        maxAttempts = 3
    )

    logger.info { "Security context verified. Processing transaction for purpose: $purpose" }
    body(actionPrincipal)

    if (purpose == ActionConfirmationPurpose.DELETE_RESOURCES || purpose == ActionConfirmationPurpose.TRANSFER_STOCK) {
        alertService.dispatchImmediateSecurityAlert(
            organizationId = actionPrincipal.organizationId,
            workspaceId = actionPrincipal.workspaceId,
            action = "CRITICAL_ACTION_EXECUTED",
            summary = "Action: ${purpose.name}. Target: $resourceName. Operator: ${actionPrincipal.username}. Notes: $auditSummary. Request ID: ${auditContext.requestId}"
        )
    }
}
