package com.citacomsys.app.shared.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.citacomsys.core.notification.system.AppNotificationManager
import com.citacomsys.core.notification.system.NotificationActionRegistry

/**
 * Global observer and handler for [AppNotificationManager.actionEvents].
 * Automatically dispatches notification action clicks to their registered lambdas
 * via [NotificationActionRegistry], letting background services handle everything silently.
 */
@Composable
fun NotificationActionHandler() {
    LaunchedEffect(Unit) {
        AppNotificationManager.actionEvents.collect { (actionId, notification) ->
            val handled = NotificationActionRegistry.dispatch(actionId, notification)
            if (!handled) {
                AppNotificationManager.removeNotification(notification.id)
            }
        }
    }
}
