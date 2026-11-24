package com.citacomsys.feature.api.telemetry

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class SecurityThreatSummary(
    val clientIp: String,
    val deviceId: String?,
    val appFlavor: String,
    val totalViolations: Long,
    val lastTriggeredEvent: String,
    val lastOccurredAt: Instant
)

// --- Component Failure Data Model ---
@Serializable
data class ComponentFailureSummary(
    val componentName: String,
    val totalFailures: Long,
    val exampleErrorMessage: String,
    val lastOccurredAt: Instant
)

@Serializable
data class TraceTimelineEntry(
    val requestId: String,
    val logSource: String,      // "IDENTITY", "APPLICATION", "NETWORK", "OPERATIONAL"
    val level: String,          // "INFO", "WARNING", "ERROR", "CRITICAL"
    val createdAt: Instant,
    val actionOrComponent: String, 
    val description: String,     
    val organizationId: String?,
    val workspaceId: String?,
    val deviceId: String?
)

// --- THE COMMON BASE LINE METADATA ---
@Serializable
data class LogMetadata(
    val id: Long? = null,
    val requestId: String,
    val orgId: String? = null,
    val workspaceId: String? = null,
    val branchId: String? = null,
    val deviceId: String? = null,
    val appFlavor: String,
    val level: TelemetryLevel = TelemetryLevel.INFO,
    val createdAt: Instant = Clock.System.now()
)

@Serializable
enum class TelemetryLevel { INFO, WARNING, ERROR, CRITICAL }

// --- IDENTITY & ACCESS EVENT ---
@Serializable
data class IdentityLog(
    val metadata: LogMetadata,
    val userId: String,
    val action: String, // LOGIN, LOGOUT, REFRESH_TOKEN
    val outcome: String, // SUCCESS, FAILURE
    val failureReason: String? = null
)

// --- ENDPOINT & HOST LOG ---
@Serializable
data class EndpointHostLog(
    val metadata: LogMetadata,
    val osName: String,
    val architecture: String,
    val action: SecurityActionEvent, // DEVICE_BINDING, REFRESH_TAMPER_DETECTED
    val fingerprintHash: String
)

// --- APPLICATION & TENANT AUDIT LOG ---
@Serializable
data class ApplicationLog(
    val metadata: LogMetadata,
    val actorId: String, // The user executing the action
    val entityType: String, // INVENTORY, USER, BILLING
    val entityId: String,
    val action: String, // CREATE, UPDATE, DELETE
    val summary: String,
    val payloadChanges: JsonElement? = null // Before/After state differences
)

// --- NETWORK & PERIMETER SECURITY LOG ---
@Serializable
data class NetworkLog(
    val metadata: LogMetadata,
    val clientIp: String,
    val endpointRoute: String,
    val triggerEvent: String, // RATE_LIMIT_EXCEEDED, BLOCKED_OTP_ATTEMPT
    val blockedDurationSeconds: Int? = null
)

// --- OPERATIONAL & AVAILABILITY HEALTH LOG ---
@Serializable
data class OperationalLog(
    val metadata: LogMetadata,
    val component: String, // "KtorHttpClient", "PostgresConnectionPool"
    val message: String,
    val executionTimeMs: Long? = null,
    val stack_trace: String? = null
)

@Serializable
enum class SecurityActionEvent {
    REFRESH_REPLAY_DETECTED,
    REFRESH_TAMPER_DETECTED,
    ACCOUNT_LOCKOUT,
    DEVICE_BINDING,
    DEVICE_FAST_TRACK_RECOVERY,
    USER_SESSION_REVOKED,
    DEVICE_MISMATCH_DETECTED,
    USER_INACTIVE_ACCESS_ATTEMPT,
    DEVICE_REVOKED_ACCESS_ATTEMPT,
    RESET_PASSWORD,
    CHANGED_PASSWORD,
    WORKSPACE_ACTIVATED,
    ORGANIZATION_REGISTERED
}
