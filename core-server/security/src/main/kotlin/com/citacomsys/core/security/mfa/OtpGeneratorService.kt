package com.citacomsys.core.security.mfa

import java.security.SecureRandom
import java.security.MessageDigest
import java.util.Base64

interface OtpGeneratorService {
    data class GeneratedOtp(val rawCode: String, val hashedCode: String)
    fun generate(): GeneratedOtp
    fun hash(code: String): String
}

internal class SecureOtpGeneratorServiceImpl : OtpGeneratorService {
    private val random = SecureRandom()

    override fun generate(): OtpGeneratorService.GeneratedOtp {
        // Generates a cryptographically secure 6-digit code (000000 - 999999)
        val code = String.format("%06d", random.nextInt(1000000))
        return OtpGeneratorService.GeneratedOtp(
            rawCode = code,
            hashedCode = hash(code)
        )
    }

    override fun hash(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(code.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
