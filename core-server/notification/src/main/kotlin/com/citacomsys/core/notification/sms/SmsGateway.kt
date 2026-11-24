package com.citacomsys.core.notification.sms

import com.citacomsys.core.notification.sms.notification.SmsResult
import com.citacomsys.core.notification.sms.notification.SmsStatusResponse

interface SmsGateway {
    suspend fun send(phone: String, message: String): SmsResult
    suspend fun getStatus(messageId: String): Result<SmsStatusResponse>
}