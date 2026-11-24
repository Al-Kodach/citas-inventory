package com.citacomsys.core.notification.email

import com.citacomsys.core.notification.backgroundJobHandler.BackgroundJobHandler
import kotlinx.serialization.json.JsonObject

interface EmailService : BackgroundJobHandler {
    suspend fun sendInstant(payload: JsonObject): Boolean
    suspend fun sendScheduled(payload: JsonObject): Boolean
}
