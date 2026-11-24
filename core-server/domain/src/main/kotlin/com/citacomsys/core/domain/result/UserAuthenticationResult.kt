package com.citacomsys.core.domain.result

import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.User

/**
 * A dedicated result category for the UserAuthenticationService to clearly
 * distinguish between different failure modes.
 */
sealed class AuthenticationResult {
    data class Success(val user: User) : AuthenticationResult()
    sealed class Failure(
        open val message: String,
        open val cause: Throwable? = null
    ) : AuthenticationResult()

    data class InvalidCredentials(
        override val message: String = "Invalid credentials",
        override val cause: Throwable? = null
    ) : Failure(message, cause)

    data class AccountBlocked(override val message: String, override val cause: Throwable? = null) :
        Failure(message, cause)

    data class AccountInactive(
        override val message: String = "Account is inactive",
        override val cause: Throwable? = null
    ) : Failure(message, cause)

    data class TenantInactive(
        override val message: String = "Business account is inactive or suspended",
        override val cause: Throwable? = null
    ) : Failure(message, cause)

    data class PhoneVerificationRequired(
        val userId: String,
        val username: String,
        val organizationId: String,
        val email: String?,
        val phone: PhoneNumber?,
        override val message: String = "Phone verification required",
        override val cause: Throwable? = null
    ) : Failure(message, cause) {
        val accMasked = email ?: phone?.mask
    }

    data class AccountVerificationRequired(
        val userId: String,
        val username: String,
        val organizationId: String,
        val email: String?,
        val phone: PhoneNumber?,
        override val message: String = "Account verification required",
        override val cause: Throwable? = null
    ) : Failure(message, cause) {
        val accMasked = email ?: phone?.mask
    }
}

/**
 * A specific exception that is thrown when the UserAuthenticationService fails.
 * It wraps the detailed AuthenticationResult, allowing a centralized handler
 * to inspect the specific reason for the failure.
 */
class UserAuthenticationException(val result: AuthenticationResult.Failure) :
    RuntimeException(result.message)
