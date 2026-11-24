package com.citacomsys.core.notification

import com.logging.AppNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val isCritical: Boolean
)

object PlatformNotificationHelper {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val notificationCount: StateFlow<Int> =
        AppNotificationManager.notifications.map { it.size }
            .stateIn(scope, SharingStarted.Eagerly, 0)

    val hasCritical: StateFlow<Boolean> =
        AppNotificationManager.notifications.map { list -> list.any { it.isCritical } }
            .stateIn(scope, SharingStarted.Eagerly, false)

    fun getNotifications(): List<NotificationItem> {
        return AppNotificationManager.notifications.value.map {
            NotificationItem(
                id = it.id,
                title = it.title,
                message = it.message,
                isCritical = it.isCritical
            )
        }
    }

    fun dismiss(id: String) {
        AppNotificationManager.clearAll()
    }

    fun clearAll() {
        AppNotificationManager.clearAll()
    }
}
