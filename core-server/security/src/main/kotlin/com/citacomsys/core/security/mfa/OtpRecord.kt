package com.citacomsys.core.security.mfa

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class OtpRecord(
    val id: String,
    val userId: String,
    val requestId: String,
    val prefix: String = "OTP",
    val purpose: String,
    val hashedCode: String,
    val expiresAt: Instant,
    val isUsed: Boolean = false,
    val isCancelled: Boolean = false
)
