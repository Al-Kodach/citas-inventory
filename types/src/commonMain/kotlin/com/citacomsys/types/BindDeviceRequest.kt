package com.citacomsys.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@SerialName("BindDeviceRequest")
data class BindDeviceRequest(
    val sessionId: String,
    val identifier: String,
    val deviceId: String,
    val deviceName: String,
    val metadata: Map<String, String> = emptyMap(),
    val otpCode: String? = null // Nullable: Step 1 (Request), Non-null: Step 2 (Verify)
)

@Serializable
sealed class BindDeviceResponse {

    @Serializable
    @SerialName("otp_required")
    data class OtpRequired(
        val sessionId: String, // To track the binding session
        val maskedIdentifier: String, // e.g., +233XXXXXX90
        val resendIntervalSeconds: Int = 60
    ) : BindDeviceResponse()

    @Serializable
    @SerialName("awaiting_approval")
    data class AwaitingApproval(
        val sessionId: String,
        val deviceId: String,
        val message: String = "This device is awaiting administrative approval."
    ) : BindDeviceResponse()

    @Serializable
    @SerialName("success")
    data class Success(
        val deviceToken: String,
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val tailscaleAuthKey: String? = null
    ) : BindDeviceResponse()
}