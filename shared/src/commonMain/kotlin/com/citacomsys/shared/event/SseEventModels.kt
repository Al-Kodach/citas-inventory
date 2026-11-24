package com.citacomsys.shared.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ClientType {
    ADMIN_PLATFORM,
    MOTHER_HUB,
    POS_TERMINAL
}

@Serializable
@SerialName("SseMessage")
data class SseMessage(
    val event: String,
    val data: String,
    val topic: String,
    override val organizationId: String,
    val workspaceId: String? = null,
    val branchId: String? = null
) : CitasEvent {
    companion object {
        const val name = "event.sse.message"
    }
}

@Serializable
@SerialName("SseSubscriptionRequest")
data class SseSubscriptionRequest(
    val clientId: String = "",
    val workspaceId: String?,
    override val organizationId: String = "",
    val branchId: String?,
    val topics: Set<String>,
    val clientType: ClientType
) : CitasEvent {
    companion object {
        const val name = "event.subscription.request"
    }
}

@Serializable
@SerialName("SseClientState")
data class SseClient(
    val id: String,
    val organizationId: String,
    val workspaceId: String?,
    val branchId: String? = null,
    val topics: Set<String> = emptySet(),
    val clientType: ClientType = ClientType.POS_TERMINAL
)
