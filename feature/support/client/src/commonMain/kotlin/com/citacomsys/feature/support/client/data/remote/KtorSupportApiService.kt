package com.citacomsys.feature.support.client.data.remote

import com.citacomsys.feature.support.api.AddMessageRequest
import com.citacomsys.feature.support.api.CreateTicketRequest
import com.citacomsys.feature.support.api.SupportMessageRecord
import com.citacomsys.feature.support.api.SupportTicketRecord
import com.citacomsys.feature.support.api.TicketStatus
import com.citacomsys.feature.support.api.service.SupportApiService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorSupportApiService(
    private val httpClient: HttpClient
) : SupportApiService {

    override suspend fun getTickets(workspaceId: String): List<SupportTicketRecord> {
        return httpClient.get("/api/support/tickets") {
            parameter("activeWorkspaceId", workspaceId)
        }.body()
    }

    override suspend fun getTicket(ticketId: String): SupportTicketRecord? {
        return try {
            httpClient.get("/api/support/tickets/$ticketId").body()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createTicket(request: CreateTicketRequest): SupportTicketRecord {
        return httpClient.post("/api/support/tickets") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun addMessage(request: AddMessageRequest): SupportMessageRecord {
        return httpClient.post("/api/support/tickets/message") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun updateTicketStatus(ticketId: String, status: TicketStatus): SupportTicketRecord {
        return httpClient.patch("/api/support/tickets/$ticketId/status") {
            parameter("status", status.name)
        }.body()
    }
}
