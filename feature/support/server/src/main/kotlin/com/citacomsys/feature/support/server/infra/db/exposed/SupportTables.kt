package com.citacomsys.feature.support.server.infra.db.exposed

import org.jetbrains.exposed.v1.core.Table

object SupportTicketsTable : Table("support_tickets") {
    val ticketId = varchar("ticket_id", 64)
    val workspaceId = varchar("workspace_id", 64)
    val userId = varchar("user_id", 64)
    val userEmail = varchar("user_email", 128)
    val title = varchar("title", 256)
    val description = text("description")
    val type = varchar("type", 32)
    val priority = varchar("priority", 32)
    val status = varchar("status", 32)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val rating = integer("rating").nullable()
    val appVersion = varchar("app_version", 32).nullable()
    val deviceModel = varchar("device_model", 64).nullable()

    override val primaryKey = PrimaryKey(ticketId)
}

object SupportMessagesTable : Table("support_messages") {
    val messageId = varchar("message_id", 64)
    val ticketId = varchar("ticket_id", 64)
    val senderId = varchar("sender_id", 64)
    val senderName = varchar("sender_name", 128)
    val content = text("content")
    val timestamp = long("timestamp")
    val isSupportAgent = bool("is_support_agent")

    override val primaryKey = PrimaryKey(messageId)
}
