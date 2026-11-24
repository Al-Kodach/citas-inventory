package com.citacomsys.core.notification.system

/**
 * Registry allowing features (such as mesh, sync, inventory, settings) to register
 * custom action lambdas dynamically for notification buttons.
 * When a user clicks an action on a notification, the registered lambda is executed.
 */
object NotificationActionRegistry {
    private val handlers = mutableMapOf<String, (AppNotification) -> Unit>()

    fun register(actionId: String, handler: (AppNotification) -> Unit) {
        handlers[actionId] = handler
    }

    fun unregister(actionId: String) {
        handlers.remove(actionId)
    }

    fun dispatch(actionId: String, notification: AppNotification): Boolean {
        val handler = handlers[actionId]
        if (handler != null) {
            handler(notification)
            return true
        }
        return false
    }
}
