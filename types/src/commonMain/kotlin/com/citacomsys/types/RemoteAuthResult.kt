package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MFAChannel {
    SMS, EMAIL
}

@Serializable
enum class ChallengeType {
    PASSWORD,
    OTP,
    BIOMETRIC,
    PIN,
    ACTION_CONFIRMATION_TOKEN
}

@Serializable
@SerialName("AuthenticatedSession")
data class AuthenticatedSession(
    val tokens: AuthTokens,
    val user: User,
    val workspaceBindings: List<UserWorkspaceBinding>
)

@Serializable
data class AuthChallengeContext(
    val challengeId: String = "",
    val userId: String = "",
    val organizationId: String = "",
    val action: String = "",
    val challengeType: ChallengeType = ChallengeType.OTP,
    val title: String = "Verify Identity",
    val description: String = "",
    val accountMasked: String? = null,
    val otpPurpose: String = "",
    val allowEmergencyRecovery: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val channel: MFAChannel
) {
    val codeLength: Int get() = if (channel == MFAChannel.EMAIL) 6 else 4
}

@Serializable
sealed interface RemoteAuthResult {
    @Serializable
    @SerialName("Success")
    data class Success(val session: AuthenticatedSession) : RemoteAuthResult

    @Serializable
    @SerialName("VerificationRequired")
    data class VerificationRequired(val challenge: AuthChallengeContext) : RemoteAuthResult

    @Serializable
    @SerialName("MfaRequired")
    data class MfaRequired(val challenge: AuthChallengeContext) : RemoteAuthResult

    @Serializable
    @SerialName("UnregisteredDevice")
    data object UnregisteredDevice : RemoteAuthResult

    @Serializable
    @SerialName("DeviceLocked")
    data object DeviceLocked : RemoteAuthResult
}
