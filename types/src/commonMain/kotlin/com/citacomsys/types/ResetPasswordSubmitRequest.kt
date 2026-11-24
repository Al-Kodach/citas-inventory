package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
data class ResetPasswordSubmitRequest(
    val challenge: AuthChallengeContext,
    val otp: String,
    val newPassword: String
)
