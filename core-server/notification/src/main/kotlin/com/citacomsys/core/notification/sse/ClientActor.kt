package com.citacomsys.core.notification.sse

import com.citacomsys.shared.event.SseClient
import com.citacomsys.shared.event.SseMessage
import io.ktor.server.sse.ServerSSESession
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class ClientActor(
    val client: SseClient,
    val session: ServerSSESession,
    val userId: String = "",
    private val onClose: suspend (SseClient) -> Unit
) {
    private val channel = Channel<SseMessage>(
        capacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * The sending loop is launched directly on the session's scope.
     * This ensures that Ktor's SSE handler remains active and flushes correctly.
     */
    val job: Job = session.launch {
        try {
            // Send an immediate "connected" event to flush the stream
            session.send(event = "connection_established", data = "{\"status\":\"ok\"}")
            
            for (msg in channel) {
                if (!isActive) break
                try {
                    session.send(data = msg.data, event = msg.event)
                } catch (e: Exception) {
                    // Log and break to trigger cleanup
                    break
                }
            }
        } finally {
            onClose(client)
        }
    }

    fun trySend(message: SseMessage): Boolean {
        return channel.trySend(message).isSuccess
    }
}
