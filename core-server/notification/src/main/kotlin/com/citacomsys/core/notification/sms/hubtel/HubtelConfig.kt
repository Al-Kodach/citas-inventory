package com.citacomsys.core.notification.sms.hubtel

import mu.KotlinLogging

internal val logger = KotlinLogging.logger("SmsService")
internal data class HubtelConfig(
    val clientId: String,
    val clientSecret: String,
    val senderId: String,
    val senderName: String,
)