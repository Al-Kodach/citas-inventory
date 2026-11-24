package com.citacomsys.core.notification.sse

import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.core.notification.service.SseService
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.event.SubscriptionEvent
import com.citacomsys.shared.event.UserCountEvent
import com.citacomsys.shared.event.SseClient
import com.citacomsys.shared.event.SseMessage
import com.citacomsys.shared.event.SseSubscriptionRequest
import com.citacomsys.shared.event.ClientType
import com.citacomsys.shared.event.UserSessionRevokedEvent
import io.ktor.server.sse.ServerSSESession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

internal val logger = KotlinLogging.logger { }

internal class SseServiceImpl(
    applicationScope: CoroutineScope
) : SseService {
    private val mutex = Mutex()
    private val clients = mutableMapOf<SseClient, ClientActor>()

    init {
        // Core background heartbeat loop
        applicationScope.launch {
            while (isActive) {
                delay(30.seconds) // Send every 30s to keep connection alive
                val heartbeatData = """{"time":"${Clock.System.now()}"}"""
                broadcast(
                    SseMessage(
                        event = "heartbeat",
                        data = heartbeatData,
                        topic = "SYSTEM",
                        organizationId = SystemActors.SYSTEM_ID
                    )
                )
            }
        }

        // Subscribe to Subscription events
        EventBus.subscribe<SubscriptionEvent>().onEach { event ->
            broadcast(
                SseMessage(
                    event = SubscriptionEvent.ID,
                    data = Json.encodeToString(event),
                    topic = "SUBSCRIPTION",
                    organizationId = event.organizationId,
                    workspaceId = event.wsId
                )
            )
        }.launchIn(applicationScope)

        // Subscribe to Entity Change events with Branch & Workspace scoping
        EventBus.subscribe<EntityChangeEvent>().onEach { event ->
            val wsId = event.workspaceId ?: event.organizationId
            val branchId = event.branchId

            val topic = when (event.entityType.uppercase()) {
                "BILLING" -> "BILLING"
                "USER", "TENANT" -> "OPERATIONS"
                "SECURITY" -> "SECURITY"
                else -> "INVENTORY_STOCK"
            }

            broadcast(
                SseMessage(
                    event = EntityChangeEvent.name,
                    data = Json.encodeToString(event),
                    topic = topic,
                    organizationId = event.organizationId,
                    workspaceId = wsId,
                    branchId = branchId
                )
            )
        }.launchIn(applicationScope)

        // Subscribe to User Session Revoked Security Events (Force logout client signal)
        EventBus.subscribe<UserSessionRevokedEvent>().onEach { event ->
            broadcast(
                SseMessage(
                    event = UserSessionRevokedEvent.name,
                    data = Json.encodeToString(event),
                    topic = "SECURITY",
                    organizationId = event.organizationId,
                    workspaceId = event.hardwareId
                )
            )
        }.launchIn(applicationScope)

        // Subscribe to Dynamic Subscription Request commands
        EventBus.subscribe<SseSubscriptionRequest>().onEach { request ->
            subscribe(request)
        }.launchIn(applicationScope)
    }

    override suspend fun addClient(client: SseClient, session: ServerSSESession) {
        mutex.withLock {
            // Prevent session duplication
            clients.entries.find { it.key.id == client.id }?.let { (oldKey, oldActor) ->
                logger.info { "[SSE] Reconnection for ${client.id}. Closing old session." }
                oldActor.job.cancel()
                clients.remove(oldKey)
            }

            val actor = ClientActor(client, session) { removeClient(client.id) }
            clients[client] = actor

            actor.trySend(
                SseMessage(
                    event = "connection_established",
                    data = """{"status":"ok"}""",
                    topic = "SYSTEM",
                    organizationId = client.organizationId,
                    workspaceId = client.workspaceId
                )
            )
            logger.info { "[SSE] Client connected: ${client.id} (Type: ${client.clientType}, Topics: ${client.topics}). Active: ${clients.size}" }
        }
    }

    override suspend fun removeClient(deviceId: String) {
        val client = clients.keys.firstOrNull { it.id == deviceId } ?: return

        val removedActor = mutex.withLock {
            clients.remove(client)
        }

        if (removedActor != null) {
            removedActor.job.cancel()
            logger.info { "[SSE] Client disconnected: ${client.id}. Active: ${clients.size}" }
        }
    }

    override suspend fun subscribe(request: SseSubscriptionRequest) {
        mutex.withLock {
            val client = SseClient(
                id = request.clientId,
                organizationId = request.organizationId,
                workspaceId = request.workspaceId,
                branchId = request.branchId,
                topics = request.topics,
                clientType = request.clientType
            )
            val existingEntry = clients.entries.find { it.key.id == client.id }
            if (existingEntry != null) {
                val actor = clients.remove(existingEntry.key)
                if (actor != null) {
                    clients[client] = actor
                }
            } else {
                val oldKey = clients.keys.firstOrNull { it.id == client.id }
                if (oldKey != null) {
                    val actor = clients.remove(oldKey)
                    if (actor != null) clients[client] = actor
                }
            }
            logger.info { "[SSE] Subscription registered/updated for client ${client.id} (Topics: ${client.topics}, WS: ${client.workspaceId}, Branch: ${client.branchId})" }
        }
    }

    override suspend fun broadcast(message: SseMessage) {
        val snapshot = mutex.withLock { clients.values.toList() }
        val systemId = SystemActors.SYSTEM_ID

        snapshot.forEach { actor ->
            val client = actor.client

            // 1. Topic Match Check
            val matchesTopic = client.topics.isEmpty() ||
                client.topics.contains(message.topic) ||
                message.topic == "SYSTEM" ||
                message.topic == "SECURITY" ||
                client.clientType == ClientType.MOTHER_HUB ||
                (client.clientType == ClientType.ADMIN_PLATFORM && message.topic == "TENANT_ADMIN")

            if (!matchesTopic) return@forEach

            // 2. Hierarchical Tenant, Workspace, and Branch Scope Match Check
            val matchesScope = when {
                // System-wide broadcast
                message.organizationId == systemId -> true

                // Organization-wide match
                message.workspaceId.isNullOrBlank() -> {
                    client.organizationId == message.organizationId || client.organizationId == systemId
                }

                // Workspace-wide match (branchId is null/blank)
                message.branchId.isNullOrBlank() -> {
                    client.workspaceId == message.workspaceId ||
                    (client.clientType == ClientType.MOTHER_HUB && client.organizationId == message.organizationId) ||
                    client.organizationId == systemId
                }

                // Branch-specific match
                else -> {
                    (client.workspaceId == message.workspaceId && (client.branchId == message.branchId || client.branchId == null)) ||
                    (client.clientType == ClientType.MOTHER_HUB && client.organizationId == message.organizationId) ||
                    client.organizationId == systemId
                }
            }

            if (matchesScope) {
                if (!actor.trySend(message)) {
                    logger.debug { "Failed to queue message ${message.event} for ${actor.client.id}" }
                }
            }
        }
    }

    override suspend fun disconnect(deviceId: String) {
        mutex.withLock {
            clients.entries.find { it.key.id == deviceId }?.let { (key, actor) ->
                actor.job.cancel()
                clients.remove(key)
            }
        }
    }
}
