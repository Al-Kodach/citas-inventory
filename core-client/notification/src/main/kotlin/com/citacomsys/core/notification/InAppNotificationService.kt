package com.citacomsys.core.notification

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

interface InAppNotificationService {
    val currentNotification: StateFlow<InAppNotification?>
    fun show(notification: InAppNotification): Job
    suspend fun dismiss()
}
