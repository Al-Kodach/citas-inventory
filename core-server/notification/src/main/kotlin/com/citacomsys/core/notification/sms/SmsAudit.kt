package com.citacomsys.core.notification.sms

import com.citacomsys.types.SmsBillMode

data class SmsAudit(
    val messageId: String,
    val organizationId: String,
    val workspaceId: String,
    val userId: String,
    val status: String,
    val billedFrom: SmsBillMode?
)
