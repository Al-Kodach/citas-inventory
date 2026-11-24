package com.citacomsys.core.notification

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

interface InAppNotificationService {
    val currentNotification: StateFlow<InAppNotification?>
    fun notify(notification: InAppNotification): Job
    fun add(notification: InAppNotification): Job
    fun showAlert(notification: InAppNotification)
    suspend fun dismiss()
}
