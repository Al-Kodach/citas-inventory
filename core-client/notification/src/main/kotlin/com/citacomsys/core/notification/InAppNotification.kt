package com.citacomsys.core.notification

import kotlinx.serialization.Serializable

@Serializable
data class InAppNotification(
    val id: String = "",
    val title: String = "",
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val durationMillis: Long = 4000L
)
