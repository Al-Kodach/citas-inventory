package com.citacomsys.core.notification.sms.notification

import kotlinx.serialization.Serializable

@Serializable
data class SmsResult(
    val rate: Float,
    val messageId: String,
    val status: Int,
    val statusDescription: String?,
    val networkId: String,
)

@Serializable
internal sealed class SmsJobState {
    @Serializable
    data class New(
        val userId: String,
        val organizationId: String,
        val workspaceId: String,
        val iv: ByteArray,
        val encryptedPhone: ByteArray,
        val encryptedBody: ByteArray
    ) : SmsJobState()

    @Serializable
    data class PendingCheck(
        val userId: String,
        val organizationId: String,
        val workspaceId: String,
        val messageId: String,
        val iv: ByteArray,
        val encryptedPhone: ByteArray,
        val encryptedBody: ByteArray
    ) : SmsJobState()
}
