package com.citacomsys.core.notification.service

interface SmsService {
    suspend fun send(
        userId: String,
        organizationId: String,
        workspaceId: String,
        to: String,
        body: String
    )
}