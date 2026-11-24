package com.citacomsys.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotification
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.notification.CitasNotificationType
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SecurityViolationEvent
import com.citacomsys.types.ProductType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(
    private val notificationService: InAppNotificationService,
) : ViewModel() {


    init {
        // Observe security violations globally
        viewModelScope.launch {
            EventBus.subscribe<SecurityViolationEvent>().collect { event ->
                showError("SECURITY ALERT: ${event.reason}")
                // In a real scenario, we could also update a global "isBlocked" state here
            }
        }
    }

    fun showInfo(message: String) {
        notificationService.showAlert(
            InAppNotification(message = message, type = CitasNotificationType.INFO)
        )
    }

    fun showSuccess(message: String) {
        notificationService.showAlert(
            InAppNotification(
                message = message,
                type = CitasNotificationType.SUCCESS
            )
        )
    }

    fun showError(message: String?) {
        notificationService.showAlert(
            InAppNotification(
                message = message ?: "An unexpected error occurred",
                type = CitasNotificationType.ERROR
            )
        )
    }

    fun showWarning(message: String) {
        notificationService.showAlert(
            InAppNotification(
                message = message,
                type = CitasNotificationType.WARNING
            )
        )
    }
}
