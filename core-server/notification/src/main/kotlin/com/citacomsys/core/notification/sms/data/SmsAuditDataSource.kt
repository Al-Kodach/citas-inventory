package com.citacomsys.core.notification.sms.data

import com.citacomsys.core.notification.sms.SmsAudit


internal interface SmsAuditDataSource {
    suspend fun save(audit: SmsAudit)
    suspend fun get(messageId: String): SmsAudit?
    suspend fun getAll(organizationId: String): List<SmsAudit>
    suspend fun updateStatus(messageId: String, status: String)
}