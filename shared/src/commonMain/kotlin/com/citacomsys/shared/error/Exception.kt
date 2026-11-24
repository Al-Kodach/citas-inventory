package com.citacomsys.shared.error

import kotlinx.serialization.Serializable

enum class DeviceAuthFailureReason {
    TOKEN_INVALID,      // General invalid/expired token
    DEVICE_DEACTIVATED, // Device is not active
    DEVICE_NOT_FOUND    // Device is not registered
}

@Serializable
data class SecurityErrorResponse(
    val error: String? = null,
    val retry_after_seconds: Int = 0
)

/**
 * A generic exception for authentication/authorization failures.
 */
open class AuthenticationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * A generic exception for when authorization fails.
 */
class AuthorizationException(message: String, cause: Throwable? = null) : Exception(message, cause)

open class PersistenceException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class ValidationException(val errors: Map<String, String>) :
    Exception("Validation failed. Errors: $errors")

class OptimisticLockException(val expected: Long, val actual: Long) :
    Exception("Optimistic lock failed. Expected version $expected, but found $actual.")

data class InsufficientStockException(
    override val message: String,
    val details: Map<String, String>
) : Exception(message)

data class GatewayException(override val message: String? = null) : Exception(message)