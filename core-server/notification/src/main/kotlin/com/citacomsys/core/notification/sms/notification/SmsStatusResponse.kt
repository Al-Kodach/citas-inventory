package com.citacomsys.core.notification.sms.notification

import kotlinx.serialization.Serializable

@Serializable
data class SmsStatusResponse(
    val rate: Float,
    val units: Int,
    val messageId: String,
    val content: String?,
    val status: String?, // "Sent", "Rejected", "Delivered"
    val clientReference: String?,
    val networkId: String?,
    val updateTime: String?,
    val time: String?,
    val to: String?,
    val from: String?
)

