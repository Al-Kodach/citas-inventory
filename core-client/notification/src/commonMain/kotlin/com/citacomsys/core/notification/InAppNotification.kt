package com.citacomsys.core.notification

import kotlin.time.Clock

data class InAppNotification(
    val title: String? = null,
    val message: String,
    val type: CitasNotificationType = CitasNotificationType.INFO,
    val durationMillis: Long = 4000,
    val isSystem: Boolean = false,
    val id: Long = Clock.System.now().toEpochMilliseconds()
)
