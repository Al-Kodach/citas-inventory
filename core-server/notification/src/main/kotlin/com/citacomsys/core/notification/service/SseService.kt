package com.citacomsys.core.notification.service

import com.citacomsys.shared.event.SseClient
import com.citacomsys.shared.event.SseMessage
import com.citacomsys.shared.event.SseSubscriptionRequest
import io.ktor.server.sse.ServerSSESession

interface SseService {

    suspend fun addClient(client: SseClient, session: ServerSSESession)

    suspend fun removeClient(deviceId: String)

    suspend fun broadcast(message: SseMessage)

    suspend fun subscribe(request: SseSubscriptionRequest)

    suspend fun disconnect(deviceId: String)
}
