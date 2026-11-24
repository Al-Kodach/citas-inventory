package com.citacomsys.auth.usecase

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.SecurityActionEvent
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.security.service.SecurityTrackableEvent
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.types.EntityType
import com.citacomsys.types.UserStatus
import com.citacomsys.inventory.api.user.UserRepository
import mu.KotlinLogging

internal class ChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val auditor: TelemetryAuditor,
    private val securityThrottler: SecurityThrottlerService
) {
    private val logger = KotlinLogging.logger("ChangePasswordUseCase")

    suspend fun execute(
        userId: String,
        oldPassword: String = "",
        newPassword: String,
    ): Result<Unit> = runCatching {
        val user = userRepository.findById(userId).getOrThrow()

        if (oldPassword.isNotBlank() && !passwordHasher.verify(oldPassword, user.passwordHash)) {
            securityThrottler.recordEvent(
                userId,
                SecurityTrackableEvent.FAILED_PASSWORD_ATTEMPT
            )

            auditor.logIdentityAccessEvent(
                userId = user.id,
                action = "CHANGE_PASSWORD",
                outcome = "FAILURE",
                failureReason = "INCORRECT_CURRENT_PASSWORD",
                level = TelemetryLevel.WARNING
            )
            throw IllegalArgumentException("Current password is incorrect.")
        }

        if (newPassword.length < 8) {
            auditor.logIdentityAccessEvent(
                userId = user.id,
                action = "CHANGE_PASSWORD",
                outcome = "FAILURE",
                failureReason = "INVALID_COMPLEXITY",
                level = TelemetryLevel.WARNING
            )
            throw IllegalArgumentException("New password must be at least 8 characters long.")
        }

        val newHashedPassword = passwordHasher.hash(newPassword)
        val updated = userRepository.updatePassword(userId, newHashedPassword)

        if (!updated) {
            auditor.logApplicationEvent(
                action = "CHANGE_PASSWORD_FAILED",
                entityType = EntityType.USER.name,
                entityId = userId,
                summary = "Failed to update password record for user $userId",
                level = TelemetryLevel.ERROR
            )
            throw Exception("Failed to update password.")
        }

        auditor.logIdentityAccessEvent(
            userId = user.id,
            action = "CHANGE_PASSWORD",
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )

        auditor.logApplicationEvent(
            action = SecurityActionEvent.CHANGED_PASSWORD.name,
            entityType = EntityType.USER.name,
            entityId = userId,
            summary = "User ${user.username} successfully changed their password",
            level = TelemetryLevel.INFO
        )
    }
}
