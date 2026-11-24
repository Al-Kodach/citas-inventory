package com.citacomsys.core.notification.otp

interface OtpGateway {
    suspend fun sendOtp(
        senderId: String,
        phoneNumber: String,
        countryCode: String = "GH"
    ): Result<HubtelOtpSendResponse>

    suspend fun verifyOtp(
        requestId: String,
        prefix: String,
        code: String
    ): Result<Boolean>

    suspend fun resendOtp(
        requestId: String
    ): Result<Boolean>
}
