package com.citacomsys.feature.support.api

import kotlinx.serialization.Serializable

@Serializable
enum class TicketType {
    SUPPORT,
    REVIEW,
    SUGGESTION,
    ISSUE
}

@Serializable
enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

@Serializable
enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

@Serializable
data class SupportMessageRecord(
    val messageId: String,
    val ticketId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isSupportAgent: Boolean
)

@Serializable
data class SupportTicketRecord(
    val ticketId: String,
    val workspaceId: String,
    val userId: String,
    val userEmail: String,
    val title: String,
    val description: String,
    val type: TicketType,
    val priority: TicketPriority,
    val status: TicketStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val rating: Int? = null, // For reviews (1-5 stars)
    val appVersion: String? = null,
    val deviceModel: String? = null,
    val messages: List<SupportMessageRecord> = emptyList()
)

@Serializable
data class CreateTicketRequest(
    val workspaceId: String,
    val userId: String,
    val userEmail: String,
    val title: String,
    val description: String,
    val type: TicketType,
    val priority: TicketPriority,
    val rating: Int? = null,
    val appVersion: String? = null,
    val deviceModel: String? = null
)

@Serializable
data class AddMessageRequest(
    val ticketId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val isSupportAgent: Boolean = false
)
