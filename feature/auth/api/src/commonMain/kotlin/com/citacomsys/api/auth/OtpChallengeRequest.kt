package com.citacomsys.api.auth

import com.citacomsys.types.PhoneNumber
import kotlinx.serialization.Serializable

@Serializable
data class OtpChallengeRequest(
    val phone: PhoneNumber,
    val otp: String
)
