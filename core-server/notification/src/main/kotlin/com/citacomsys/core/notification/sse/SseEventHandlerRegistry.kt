package com.citacomsys.core.notification.sse

import kotlinx.coroutines.CoroutineScope

class SseEventHandlerRegistry(
    private val handlers: List<SseEventHandler> = emptyList()
) {
    fun startAll(scope: CoroutineScope) {
        handlers.forEach { it.start(scope) }
    }
}