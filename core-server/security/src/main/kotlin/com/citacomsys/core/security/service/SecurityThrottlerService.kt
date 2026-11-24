package com.citacomsys.core.security.service

import kotlin.time.Instant

enum class SecurityTrackableEvent {
    FAILED_OTP_ATTEMPT,
    FAILED_PASSWORD_ATTEMPT,
    OTP_REQUEST_RATE,
    DEVICE_BINDING,

    MALICIOUS_CLAIM_MISMATCH,   // Token used for an incorrect purpose
    STOCK_TRANSFER_ATTEMPT,
    HIGH_RISK_RESOURCES_OPERATION_ATTEMPT
}

data class SecurityIncidentLog(
    val timestamp: Instant,
    val key: String
)

class SecurityBreachException(
    val reason: String,
    val retryAfterSeconds: Long
) : RuntimeException(reason)


interface SecurityThrottlerService {
    suspend fun recordEvent(key: String, event: SecurityTrackableEvent)

    suspend fun evaluateThreats(key: String, event: SecurityTrackableEvent)

    suspend fun attempt(
        key: String,
        event: SecurityTrackableEvent,
        maxAttempts: Int,
    )
}
