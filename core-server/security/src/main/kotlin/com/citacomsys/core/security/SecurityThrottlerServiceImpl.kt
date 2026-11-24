package com.citacomsys.core.security

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.IdentityLog
import com.citacomsys.feature.api.telemetry.NetworkLog
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.telemetryContext
import com.citacomsys.core.logging.buildMetaLog
import com.citacomsys.core.security.service.SecurityBreachException
import com.citacomsys.core.security.service.SecurityIncidentLog
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.security.service.SecurityTrackableEvent
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.HardwareMasterStatus
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.update
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Clock

internal class SecurityThrottlerServiceImpl(
    private val auditor: TelemetryAuditor
) : SecurityThrottlerService {

    private val logger = KotlinLogging.logger("com.citacomsys.SECURITY_THROTTLER")

    private val incidentRegistry =
        ConcurrentHashMap<String, ConcurrentHashMap<SecurityTrackableEvent, CopyOnWriteArrayList<SecurityIncidentLog>>>()

    override suspend fun attempt(
        key: String,
        event: SecurityTrackableEvent,
        maxAttempts: Int
    ) {
        // Evaluate threats before recording the new action attempt
        evaluateThreats(key, event)

        // No exceptions thrown? Document the fresh security incident trail
        recordEvent(key, event)
    }

    override suspend fun recordEvent(key: String, event: SecurityTrackableEvent) {
        val userEvents = incidentRegistry.computeIfAbsent(key) { ConcurrentHashMap() }
        val logs = userEvents.computeIfAbsent(event) { CopyOnWriteArrayList() }

        logs.add(SecurityIncidentLog(timestamp = Clock.System.now(), key))
        cleanExpiredLogs(logs, windowInMinutes = 15)
    }

    override suspend fun evaluateThreats(
        key: String,
        event: SecurityTrackableEvent
    ) {
        val userEvents = incidentRegistry[key] ?: return
        val logs = userEvents[event] ?: return
        val context = currentCoroutineContext().telemetryContext

        val now = Clock.System.now()
        val windowStart = now.plus(-15, DateTimeUnit.MINUTE)
        val logsInWindow = logs.filter { it.timestamp > windowStart }

        val threshold = when (event) {
            SecurityTrackableEvent.FAILED_OTP_ATTEMPT -> 5
            SecurityTrackableEvent.OTP_REQUEST_RATE -> 3
            SecurityTrackableEvent.FAILED_PASSWORD_ATTEMPT -> 5
            SecurityTrackableEvent.DEVICE_BINDING -> 5
            SecurityTrackableEvent.MALICIOUS_CLAIM_MISMATCH -> 1 // Zero tolerance for purpose forging
            SecurityTrackableEvent.STOCK_TRANSFER_ATTEMPT -> 5
            SecurityTrackableEvent.HIGH_RISK_RESOURCES_OPERATION_ATTEMPT -> 3
        }


        if (logsInWindow.size >= threshold) {
            val summary = when (event) {
                SecurityTrackableEvent.MALICIOUS_CLAIM_MISMATCH ->
                    "CRITICAL: User attempted to execute stock transfer using an unassigned action claim token purpose context."

                SecurityTrackableEvent.STOCK_TRANSFER_ATTEMPT ->
                    "WARNING: High velocity stock transfer rate-limit block activated."

                else -> "Threat threshold reached for event [${event.name}]"
            }

            logger.warn { "SECURITY ALERT: [User/Key: $key] [Device: ${context?.deviceId}] - $summary" }

            val metadata = buildMetaLog(TelemetryLevel.CRITICAL)

            val networkLog = NetworkLog(
                metadata = metadata,
                clientIp = context?.clientIp ?: "UNKNOWN_IP",
                endpointRoute = context?.endpointRoute ?: "UNKNOWN_ROUTE",
                triggerEvent = "THROTTLED_${event.name}",
                blockedDurationSeconds = 900
            )
            auditor.logNetworkSecurity(networkLog)

            // ROUTE TO DATA AUDITING & AUTOMATED PROTECTION: Document identity and claim violations clearly
            if (event == SecurityTrackableEvent.MALICIOUS_CLAIM_MISMATCH) {
                val identityLog = IdentityLog(
                    metadata = metadata,
                    userId = key,
                    action = "ILLEGAL_CLAIM_TOKEN_PRESENTATION",
                    outcome = "BLOCKED_LOCKOUT",
                    failureReason = "Threat defense caught an actor swapping action confirmation tokens across scopes. Locked out."
                )
                auditor.logIdentity(identityLog)

                // AUTOMATED PROTECTION: Auto-quarantine / lock hardware node in registry if hardwareId is present
                context?.deviceId?.let { deviceId ->
                    runCatching {
                        dbQuery {
                            HardwareRegistry.update({ HardwareRegistry.hardwareId eq deviceId }) {
                                it[masterStatus] = HardwareMasterStatus.REVOKED
                                it[securityReason] = "Automated Defense: Zero-tolerance claim token scope forging detected."
                                it[updatedAt] = Clock.System.now()
                            }
                        }
                    }
                }
            }

            throw SecurityBreachException(
                reason = "Security block triggered. Action locked for 15 minutes.",
                retryAfterSeconds = 900
            )
        }
    }


    private fun cleanExpiredLogs(
        logs: CopyOnWriteArrayList<SecurityIncidentLog>,
        windowInMinutes: Int
    ) {
        val cutOff = Clock.System.now().plus(-windowInMinutes, DateTimeUnit.MINUTE)
        logs.removeIf { it.timestamp < cutOff }
    }
}
