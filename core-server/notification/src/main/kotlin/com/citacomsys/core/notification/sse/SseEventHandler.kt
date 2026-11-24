package com.citacomsys.core.notification.sse

import kotlinx.coroutines.CoroutineScope

interface SseEventHandler {
    /**
     * Starts the listener, which subscribes to the EventBus
     * and transforms events into SseMessages.
     */
    fun start(parentScope: CoroutineScope)
}