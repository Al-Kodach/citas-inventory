package com.citacomsys.auth.service

import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.domain.result.AuthenticationResult
import com.citacomsys.core.domain.result.AuthenticationResult.*
import com.citacomsys.core.domain.result.UserAuthenticationException
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.security.service.SecurityBreachException
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.security.service.SecurityTrackableEvent
import com.citacomsys.feature.api.telemetry.telemetryContext
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.types.UserStatus
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.types.User
import kotlinx.coroutines.currentCoroutineContext
import mu.KotlinLogging

internal class UserAuthenticationService(
    private val userRepository: UserPort,
    private val organizationRepository: OrganizationRepository,
    private val securityThrottlerService: SecurityThrottlerService,
    private val auditor: TelemetryAuditor,
    private val passwordHasher: PasswordHasher
) {
    private val logger = KotlinLogging.logger {}

    suspend fun execute(identity: String, password: String): User {
        return when (val authResult = performAuthentication(identity, password)) {
            is Success -> authResult.user
            is Failure -> throw UserAuthenticationException(authResult)
        }
    }

    private suspend fun performAuthentication(
        identity: String,
        password: String
    ): AuthenticationResult {
        val context = currentCoroutineContext().telemetryContext
        val userResult = userRepository.findByIdentity(identity) // username, phone or email

        if (userResult.isFailure) {
            logger.error { "user-result: $userResult" }
            return InvalidCredentials()
        }
        val user = userResult.getOrThrow()

        val throttleKey = "login_$identity"
        try {
           // Throttle per user identity (credential guessing protection)
            securityThrottlerService.attempt(
                key = throttleKey,
                event = SecurityTrackableEvent.FAILED_PASSWORD_ATTEMPT,
                maxAttempts = 5
            )

            // Throttle per client IP (distributed botnet & credential stuffing protection)
            val clientIp = context?.clientIp
            securityThrottlerService.attempt("ip_$clientIp", SecurityTrackableEvent.FAILED_PASSWORD_ATTEMPT, maxAttempts = 20)
        } catch (e: SecurityBreachException) {
            auditor.logIdentityAccessEvent(
                userId = user.id,
                action = "LOGIN_ATTEMPT_THROTTLED",
                outcome = "FAILURE",
                failureReason = e.message
            )
           throw e
        }

        val isPasswordValid = passwordHasher.verify(password, user.passwordHash)
        if (!isPasswordValid) {
            securityThrottlerService.recordEvent(
                key = throttleKey,
                event = SecurityTrackableEvent.FAILED_PASSWORD_ATTEMPT
            )
            auditor.logIdentityAccessEvent(
                userId = user.id,
                action = "LOGIN_ATTEMPT",
                outcome = "FAILURE",
                failureReason = "Incorrect password"
            )
            return InvalidCredentials()
        }

        val organization = organizationRepository.get(user.organizationId).getOrNull()
            ?: return TenantInactive("Organization account not found.")

        // If pending, the only user in this organization is the owner
        if (organization.status == OrgStatus.PENDING_VERIFICATION) {
            auditor.logIdentityAccessEvent(
                userId = user.id,
                action = "LOGIN_ATTEMPT",
                outcome = "REQUEST_ACCOUNT_VERIFICATION"
            )
            // User == owner
            return AccountVerificationRequired(
                userId = user.id,
                username = user.username,
                email = user.email,
                organizationId = user.organizationId,
                phone = user.phone
            )
        }

        if (organization.status != OrgStatus.ACTIVE) {
            return TenantInactive()
        }

        val accountCheckResult = when (user.status) {
            UserStatus.ACTIVE -> Success(user)
            UserStatus.REQUIRED_PHONE_VERIFICATION -> {
                PhoneVerificationRequired(
                    userId = user.id,
                    username = user.username,
                    email = user.email,
                    organizationId = user.organizationId,
                    phone = user.phone
                )
            }
            UserStatus.SUSPENDED -> AccountBlocked("Your personal account has been suspended.")
            UserStatus.DELETED -> AccountInactive("This account has been deactivated.")
        }

        if (accountCheckResult is Success) {
            auditor.logIdentityAccessEvent(
                userId = user.id,
                action = "LOGIN_ATTEMPT",
                outcome = "SUCCESS"
            )
        }

        return accountCheckResult
    }
}
