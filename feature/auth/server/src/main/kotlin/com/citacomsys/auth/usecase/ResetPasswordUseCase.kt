package com.citacomsys.auth.usecase

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.SecurityActionEvent
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.core.security.service.MfaAuthInfo
import com.citacomsys.core.security.service.MfaService
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.security.service.SecurityTrackableEvent
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.types.EntityType
import com.citacomsys.inventory.api.user.UserRepository
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.PhoneNumber
import mu.KotlinLogging

internal class ResetPasswordUseCase(
    private val userRepository: UserRepository,
    private val mfaService: MfaService,
    private val passwordHasher: PasswordHasher,
    private val auditor: TelemetryAuditor,
    private val securityThrottler: SecurityThrottlerService
) {
    private val logger = KotlinLogging.logger("ResetPasswordUseCase")

    suspend fun request(identifier: String): Result<AuthChallengeContext> = runCatching {
        logger.info { "Requesting password resetWorkspaceSession for $identifier" }

        // Rate limit password resetWorkspaceSession requests by identifier (phone/email)
        securityThrottler.attempt(identifier, SecurityTrackableEvent.OTP_REQUEST_RATE, 3)

        val user = userRepository.findByPhone(identifier).getOrNull()
            ?: userRepository.findByIdentity(identifier).getOrNull()
            ?: throw IllegalArgumentException("User not found for given phone number or email address")

        val isEmailMatch = user.email?.equals(identifier, ignoreCase = true) == true
        val isPhoneMatch = user.phone.value == identifier || user.phone.normalized == PhoneNumber(identifier).normalized

        if (!isEmailMatch && !isPhoneMatch) {
            throw IllegalArgumentException("Password resetWorkspaceSession can only be requested using a registered phone number or email address")
        }

        val channel = mfaService.authAccount(
            MfaAuthInfo(
                userId = user.id,
                username = user.username,
                email = user.email,
                phone = user.phone,
                purpose = OtpPurpose.RESET_PASSWORD
            )
        ).getOrThrow()

        auditor.logIdentityAccessEvent(
            userId = user.id,
            action = "RESET_PASSWORD_REQUEST",
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )

        AuthChallengeContext(
            userId = user.id,
            organizationId = user.organizationId,
            accountMasked = user.emailMask ?: user.phoneMask,
            otpPurpose = OtpPurpose.RESET_PASSWORD.name,
            channel = channel
        )
    }

    // Update password if OTP is valid
    suspend fun submit(
        challenge: AuthChallengeContext,
        newPassword: String,
        otpCode: String
    ): Result<Unit> = runCatching {
        val userId = challenge.userId
        securityThrottler.evaluateThreats(userId, SecurityTrackableEvent.FAILED_OTP_ATTEMPT)

        val user = userRepository.findById(userId).getOrElse {
            logger.error { "User not found for password resetWorkspaceSession submission: $userId" }
            securityThrottler.recordEvent(userId, SecurityTrackableEvent.FAILED_OTP_ATTEMPT)
            throw IllegalArgumentException("Invalid request")
        }

        val otpResult = mfaService.verify(
            userId = user.id,
            code = otpCode,
            purpose = OtpPurpose.RESET_PASSWORD
        )

        if (otpResult.isFailure) {
            securityThrottler.recordEvent(userId, SecurityTrackableEvent.FAILED_OTP_ATTEMPT)
            auditor.logIdentityAccessEvent(
                userId = user.id,
                action = SecurityActionEvent.RESET_PASSWORD.name,
                outcome = "FAILURE",
                failureReason = "INVALID_OTP",
                level = TelemetryLevel.WARNING
            )
            throw otpResult.exceptionOrNull() ?: IllegalArgumentException("Invalid OTP")
        }

        val newHashedPassword = passwordHasher.hash(newPassword)
        val success = userRepository.updatePassword(user.id, newHashedPassword)

        if (!success) {
            logger.error { "Failed to update password for user ${user.id}" }
            auditor.logApplicationEvent(
                action = SecurityActionEvent.CHANGED_PASSWORD.name,
                entityType = EntityType.USER.name,
                entityId = user.id,
                summary = "Failed to persist new password during resetWorkspaceSession",
                level = TelemetryLevel.ERROR
            )
            throw IllegalStateException("Failed to update password")
        }

        auditor.logIdentityAccessEvent(
            userId = user.id,
            action = SecurityActionEvent.RESET_PASSWORD.name,
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )

        auditor.logApplicationEvent(
            action = SecurityActionEvent.CHANGED_PASSWORD.name,
            entityType = EntityType.USER.name,
            entityId = user.id,
            summary = "Password successfully resetWorkspaceSession for user ${user.id}",
            level = TelemetryLevel.INFO
        )
    }
}
