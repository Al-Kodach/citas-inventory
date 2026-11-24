package com.citacomsys.core.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object PlatformNotificationHelper {
    private val emptyFlow = MutableStateFlow(0)
    private val emptyBoolFlow = MutableStateFlow(false)
    actual val notificationCount: StateFlow<Int> = emptyFlow.asStateFlow()
    actual val hasCritical: StateFlow<Boolean> = emptyBoolFlow.asStateFlow()
    actual fun getNotifications(): List<NotificationItem> = emptyList()
    actual fun dismiss(id: String) {}
    actual fun clearAll() {}
}
