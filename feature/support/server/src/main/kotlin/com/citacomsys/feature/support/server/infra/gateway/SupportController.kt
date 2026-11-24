package com.citacomsys.feature.support.server.infra.gateway

import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.feature.support.api.AddMessageRequest
import com.citacomsys.feature.support.api.CreateTicketRequest
import com.citacomsys.feature.support.api.SupportMessageRecord
import com.citacomsys.feature.support.api.SupportTicketRecord
import com.citacomsys.feature.support.api.TicketPriority
import com.citacomsys.feature.support.api.TicketStatus
import com.citacomsys.feature.support.api.TicketType
import com.citacomsys.feature.support.server.infra.db.exposed.SupportMessagesTable
import com.citacomsys.feature.support.server.infra.db.exposed.SupportTicketsTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import java.util.UUID

class SupportController {

    init {
        try {
            // Schema creation handled by server boot or persistence init
        } catch (_: Exception) {
            // Ignored
        }
    }

    suspend fun getTickets(workspaceId: String): List<SupportTicketRecord> = dbQuery {
        SupportTicketsTable.selectAll().where { SupportTicketsTable.workspaceId eq workspaceId }
            .orderBy(SupportTicketsTable.createdAt, SortOrder.DESC)
            .map { row ->
                val tId = row[SupportTicketsTable.ticketId]
                val messages = SupportMessagesTable.selectAll().where { SupportMessagesTable.ticketId eq tId }
                    .orderBy(SupportMessagesTable.timestamp, SortOrder.ASC)
                    .map { mRow ->
                        SupportMessageRecord(
                            messageId = mRow[SupportMessagesTable.messageId],
                            ticketId = mRow[SupportMessagesTable.ticketId],
                            senderId = mRow[SupportMessagesTable.senderId],
                            senderName = mRow[SupportMessagesTable.senderName],
                            content = mRow[SupportMessagesTable.content],
                            timestamp = mRow[SupportMessagesTable.timestamp],
                            isSupportAgent = mRow[SupportMessagesTable.isSupportAgent]
                        )
                    }

                SupportTicketRecord(
                    ticketId = tId,
                    workspaceId = row[SupportTicketsTable.workspaceId],
                    userId = row[SupportTicketsTable.userId],
                    userEmail = row[SupportTicketsTable.userEmail],
                    title = row[SupportTicketsTable.title],
                    description = row[SupportTicketsTable.description],
                    type = TicketType.valueOf(row[SupportTicketsTable.type]),
                    priority = TicketPriority.valueOf(row[SupportTicketsTable.priority]),
                    status = TicketStatus.valueOf(row[SupportTicketsTable.status]),
                    createdAt = row[SupportTicketsTable.createdAt],
                    updatedAt = row[SupportTicketsTable.updatedAt],
                    rating = row[SupportTicketsTable.rating],
                    appVersion = row[SupportTicketsTable.appVersion],
                    deviceModel = row[SupportTicketsTable.deviceModel],
                    messages = messages
                )
            }
    }

    suspend fun getTicket(ticketId: String): SupportTicketRecord? = dbQuery {
        SupportTicketsTable.selectAll().where { SupportTicketsTable.ticketId eq ticketId }
            .map { row ->
                val tId = row[SupportTicketsTable.ticketId]
                val messages = SupportMessagesTable.selectAll().where { SupportMessagesTable.ticketId eq tId }
                    .orderBy(SupportMessagesTable.timestamp, SortOrder.ASC)
                    .map { mRow ->
                        SupportMessageRecord(
                            messageId = mRow[SupportMessagesTable.messageId],
                            ticketId = mRow[SupportMessagesTable.ticketId],
                            senderId = mRow[SupportMessagesTable.senderId],
                            senderName = mRow[SupportMessagesTable.senderName],
                            content = mRow[SupportMessagesTable.content],
                            timestamp = mRow[SupportMessagesTable.timestamp],
                            isSupportAgent = mRow[SupportMessagesTable.isSupportAgent]
                        )
                    }

                SupportTicketRecord(
                    ticketId = tId,
                    workspaceId = row[SupportTicketsTable.workspaceId],
                    userId = row[SupportTicketsTable.userId],
                    userEmail = row[SupportTicketsTable.userEmail],
                    title = row[SupportTicketsTable.title],
                    description = row[SupportTicketsTable.description],
                    type = TicketType.valueOf(row[SupportTicketsTable.type]),
                    priority = TicketPriority.valueOf(row[SupportTicketsTable.priority]),
                    status = TicketStatus.valueOf(row[SupportTicketsTable.status]),
                    createdAt = row[SupportTicketsTable.createdAt],
                    updatedAt = row[SupportTicketsTable.updatedAt],
                    rating = row[SupportTicketsTable.rating],
                    appVersion = row[SupportTicketsTable.appVersion],
                    deviceModel = row[SupportTicketsTable.deviceModel],
                    messages = messages
                )
            }.singleOrNull()
    }

    suspend fun createTicket(request: CreateTicketRequest): SupportTicketRecord = dbQuery {
        val ticketId = "TKT-${UUID.randomUUID().toString().take(8).uppercase()}"
        val now = System.currentTimeMillis()

        SupportTicketsTable.insert {
            it[SupportTicketsTable.ticketId] = ticketId
            it[SupportTicketsTable.workspaceId] = request.workspaceId
            it[SupportTicketsTable.userId] = request.userId
            it[SupportTicketsTable.userEmail] = request.userEmail
            it[SupportTicketsTable.title] = request.title
            it[SupportTicketsTable.description] = request.description
            it[SupportTicketsTable.type] = request.type.name
            it[SupportTicketsTable.priority] = request.priority.name
            it[SupportTicketsTable.status] = TicketStatus.OPEN.name
            it[SupportTicketsTable.createdAt] = now
            it[SupportTicketsTable.updatedAt] = now
            it[SupportTicketsTable.rating] = request.rating
            it[SupportTicketsTable.appVersion] = request.appVersion
            it[SupportTicketsTable.deviceModel] = request.deviceModel
        }

        val messageId = "MSG-${UUID.randomUUID().toString().take(8).uppercase()}"
        SupportMessagesTable.insert {
            it[SupportMessagesTable.messageId] = messageId
            it[SupportMessagesTable.ticketId] = ticketId
            it[SupportMessagesTable.senderId] = request.userId
            it[SupportMessagesTable.senderName] = request.userEmail
            it[SupportMessagesTable.content] = request.description
            it[SupportMessagesTable.timestamp] = now
            it[SupportMessagesTable.isSupportAgent] = false
        }

        getTicket(ticketId)!!
    }

    suspend fun addMessage(request: AddMessageRequest): SupportMessageRecord = dbQuery {
        val messageId = "MSG-${UUID.randomUUID().toString().take(8).uppercase()}"
        val now = System.currentTimeMillis()

        SupportMessagesTable.insert {
            it[SupportMessagesTable.messageId] = messageId
            it[SupportMessagesTable.ticketId] = request.ticketId
            it[SupportMessagesTable.senderId] = request.senderId
            it[SupportMessagesTable.senderName] = request.senderName
            it[SupportMessagesTable.content] = request.content
            it[SupportMessagesTable.timestamp] = now
            it[SupportMessagesTable.isSupportAgent] = request.isSupportAgent
        }

        SupportTicketsTable.update({ SupportTicketsTable.ticketId eq request.ticketId }) {
            it[SupportTicketsTable.updatedAt] = now
        }

        SupportMessageRecord(
            messageId = messageId,
            ticketId = request.ticketId,
            senderId = request.senderId,
            senderName = request.senderName,
            content = request.content,
            timestamp = now,
            isSupportAgent = request.isSupportAgent
        )
    }

    suspend fun updateStatus(ticketId: String, status: TicketStatus): SupportTicketRecord = dbQuery {
        val now = System.currentTimeMillis()
        SupportTicketsTable.update({ SupportTicketsTable.ticketId eq ticketId }) {
            it[SupportTicketsTable.status] = status.name
            it[SupportTicketsTable.updatedAt] = now
        }
        getTicket(ticketId)!!
    }
}
