package com.citacomsys.core.security.service

import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.types.MFAChannel
import com.citacomsys.types.PhoneNumber

data class MfaAuthInfo(
    val userId: String,
    val username: String,
    val email: String?,
    val phone: PhoneNumber?,
    val purpose: OtpPurpose
)

/**
 * Simplified OTP service focused on lean lifecycle management.
 */
interface MfaService {
    suspend fun authAccount(info: MfaAuthInfo): Result<MFAChannel>
    /**
     * Generates a new code and sends it.
     */
    suspend fun send(
        userId: String,
        phone: PhoneNumber,
        purpose: OtpPurpose,
    ): Result<Unit>

    /**
     * Validates the latest pending code for the user.
     */
    suspend fun verify(
        userId: String,
        code: String,
        purpose: OtpPurpose
    ): Result<Unit>
}
