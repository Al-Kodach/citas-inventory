package com.citacomsys.core.security.mfa

import com.citacomsys.core.security.mfa.data.OtpDataStore
import kotlin.time.Clock

internal class OtpValidationService(
    private val otpDataStore: OtpDataStore,
    private val otpGenerator: OtpGeneratorService
) {
    suspend fun verify(
        userId: String,
        code: String,
        purpose: OtpPurpose
    ): Result<Unit> = runCatching {
        val recode = otpDataStore.findLatestPending(userId, purpose.name)
            ?: throw IllegalArgumentException("No pending verification found. Please request a new code.")

        if (recode.expiresAt < Clock.System.now()) {
            otpDataStore.markAsCancelled(recode.id)
            throw IllegalArgumentException("Verification code has expired.")
        }

        val inputHash = otpGenerator.hash(code)

        if (inputHash != recode.hashedCode) {
            throw IllegalArgumentException("Invalid verification code.")
        }

        otpDataStore.markAsUsed(recode.id)
    }
}