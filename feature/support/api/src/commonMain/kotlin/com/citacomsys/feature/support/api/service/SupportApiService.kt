package com.citacomsys.feature.support.api.service

import com.citacomsys.feature.support.api.AddMessageRequest
import com.citacomsys.feature.support.api.CreateTicketRequest
import com.citacomsys.feature.support.api.SupportMessageRecord
import com.citacomsys.feature.support.api.SupportTicketRecord

interface SupportApiService {
    suspend fun getTickets(workspaceId: String): List<SupportTicketRecord>
    suspend fun getTicket(ticketId: String): SupportTicketRecord?
    suspend fun createTicket(request: CreateTicketRequest): SupportTicketRecord
    suspend fun addMessage(request: AddMessageRequest): SupportMessageRecord
    suspend fun updateTicketStatus(ticketId: String, status: com.citacomsys.feature.support.api.TicketStatus): SupportTicketRecord
}
