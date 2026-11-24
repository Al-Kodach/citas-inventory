package com.citacomsys.core.notification

import com.citacomsys.core.notification.system.AppNotification
import com.citacomsys.core.notification.system.AppNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class InAppNotificationServiceImpl(
    private val scope: CoroutineScope
) : InAppNotificationService {
    private val _currentNotification = MutableStateFlow<InAppNotification?>(null)
    override val currentNotification = _currentNotification.asStateFlow()

    override fun showAlert(notification: InAppNotification) {
        _currentNotification.value = notification
    }

    override fun add(notification: InAppNotification): Job = scope.launch {
        try {
            AppNotificationManager.addNotification(
                AppNotification(
                    id = notification.message.hashCode().toString(),
                    title = notification.title ?: when (notification.type) {
                        CitasNotificationType.SUCCESS -> "Success"
                        CitasNotificationType.INFO -> "Information"
                        CitasNotificationType.WARNING -> "System Warning"
                        CitasNotificationType.ERROR -> "Critical Alert"
                        CitasNotificationType.SYNCING -> "Background Sync"
                    },
                    message = notification.message,
                    isCritical = notification.type == CitasNotificationType.ERROR || notification.type == CitasNotificationType.WARNING || notification.isSystem,
                    durationMillis = notification.durationMillis
                )
            )
        } catch (_: Exception) {}
    }

    override fun notify(notification: InAppNotification): Job = scope.launch {
        add(notification)
    }

    override suspend fun dismiss() {
        _currentNotification.value = null
    }
}

object NotificationService {
    private var service: InAppNotificationService? = null

    fun notifyError(message: String) {
        val notification = InAppNotification(
            message = message,
            type = CitasNotificationType.ERROR,
            isSystem = false
        )
        service?.add(notification)
    }

    fun notifyWarning(message: String) {
        val notification = InAppNotification(
            message = message,
            type = CitasNotificationType.WARNING,
            isSystem = false
        )
        service?.add(notification)
    }

    fun notifyInfo(message: String) {
        val notification = InAppNotification(
            message = message,
            type = CitasNotificationType.INFO,
            isSystem = false
        )
        service?.add(notification)
    }

    fun notifySuccess(message: String) {
        val notification = InAppNotification(
            message = message,
            type = CitasNotificationType.SUCCESS,
            isSystem = false
        )
        service?.add(notification)
    }

    fun init(serviceImpl: InAppNotificationService) {
        service = serviceImpl
    }
}

object AlertService {
    private var service: InAppNotificationService? = null

    fun showInfo(message: String) {
        val notification = InAppNotification(
            message = message,
            type = CitasNotificationType.INFO,
            isSystem = false
        )
        service?.showAlert(notification)
    }

    fun showWarning(message: String) {
        val notification = InAppNotification(
            message = message,
            type = CitasNotificationType.WARNING,
            isSystem = false
        )
        service?.showAlert(notification)
    }

    fun showError(message: String) {
        val notification = InAppNotification(
            message = message,
            type = CitasNotificationType.ERROR,
            isSystem = false
        )
        service?.showAlert(notification)
    }

    fun showSuccess(string: String) {
        val notification = InAppNotification(
            message = string,
            type = CitasNotificationType.SUCCESS,
            isSystem = false
        )
        service?.showAlert(notification)
    }

    fun init(serviceImpl: InAppNotificationService) {
        service = serviceImpl
    }
}
