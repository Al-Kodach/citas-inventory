package com.citacomsys.core.data.service

import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.domain.service.SystemEventConnectionStatus
import com.citacomsys.core.domain.service.SystemEventService
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.BillingEvent
import com.citacomsys.shared.event.BusinessSecurityEvent
import com.citacomsys.shared.event.CitasEvent
import com.citacomsys.shared.event.ClientType
import com.citacomsys.shared.event.DeviceRevokedEvent
import com.citacomsys.shared.event.DeviceSecurityEvent
import com.citacomsys.shared.event.DeviceUpdatedEvent
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.OrgRuntimeConfig
import com.citacomsys.shared.event.SseSubscriptionRequest
import com.citacomsys.shared.event.SubscriptionEvent
import com.citacomsys.shared.event.ThemeEvent
import com.citacomsys.shared.event.UserCountEvent
import com.citacomsys.shared.event.UserSessionRevokedEvent
import com.citacomsys.shared.event.UserWorkspaceBindingEvent
import com.citacomsys.shared.event.WorkspaceEvent
import com.citacomsys.shared.event.WorkstationBindingEvent
import com.citacomsys.shared.event.WorkstationSecurityEvent
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class ClientSystemEventService(
    private val client: HttpClient,
    private val scope: CoroutineScope,
    private val prefs: PreferencesRepository,
    private val networkService: NetworkConnectivityService,
) : SystemEventService {

    private val _systemConnectionStatus = MutableStateFlow(SystemEventConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<SystemEventConnectionStatus> =
        _systemConnectionStatus.asStateFlow()

    private var sessionJob: Job? = null
    private var lastHeartbeat: Long = 0L

    init {
        scope.launch {
            combine(
                networkService.connectionStatusFlow,
                EventBus.subscribeAll(AppLifecycleEvent.WorkstationReady)
            ) { network, _ ->
                network
            }.collect {
                if (it) {
                    connect()
                } else {
                    disconnect()
                }
            }
        }
    }

    override suspend fun connect() {
        if (sessionJob?.isActive == true) return

        Napier.d { "Establishing SSE connection" }

        sessionJob?.cancel()
        sessionJob = scope.launch(Dispatchers.Default) {
            var attempt = 0
            while (isActive) {
                try {
                    _systemConnectionStatus.value = SystemEventConnectionStatus.CONNECTING

                    val currentSeq = prefs.getLastEventSeq()
                    val topics = "INVENTORY_STOCK,SUBSCRIPTION,BILLING,SECURITY,OPERATIONS"

                    client.sse(
                        urlString = "api/v1/citacomsys/inventory/events/subscribe?lastSeq=$currentSeq&topics=$topics"
                    ) {
                        Napier.i { "[SSE] Connection established. Starting from Seq: $currentSeq" }

                        _systemConnectionStatus.value = SystemEventConnectionStatus.CONNECTED
                        attempt = 0
                        lastHeartbeat = Clock.System.now().epochSeconds

                        // Send explicit subscription packet upon connection
                        launch(Dispatchers.Default) {
                            updateSubscription(
                                topics = setOf("INVENTORY_STOCK", "SUBSCRIPTION", "BILLING", "SECURITY", "OPERATIONS"),
                                workspaceId = null,
                                branchId = null
                            )
                        }

                        val watchdogJob = launch {
                            while (isActive) {
                                delay(15.seconds)
                                val elapsed = Clock.System.now().epochSeconds - lastHeartbeat
                                if (elapsed > 60) {
                                    Napier.w { "[SSE] Stream silent for ${elapsed}s — resetting connection." }
                                    break
                                }
                            }
                        }

                        incoming.collect { event ->
                            processEvent(event)
                        }

                        watchdogJob.cancel()
                    }
                } catch (e: SSEClientException) {
                    _systemConnectionStatus.value = SystemEventConnectionStatus.DISCONNECTED

                    val status = e.response?.status?.value ?: 0
                    if ((status == 400) || (status == 401)) {
                        Napier.d { "[SSE] HTTP $status response — disconnecting sse service until session/auth is restored." }
                        disconnect()
                        return@launch
                    }

                    if (isActive) {
                        val delayMs = backoffDelay(attempt)
                        Napier.e(e) { "[SSE] Connection error. Retrying in ${delayMs}ms..." }
                        delay(delayMs.milliseconds)
                        attempt++
                    }
                }
            }
        }
    }

    override suspend fun updateSubscription(
        topics: Set<String>,
        workspaceId: String?,
        branchId: String?
    ) {
        runCatching {
            val request = SseSubscriptionRequest(
                workspaceId = workspaceId,
                branchId = branchId,
                topics = topics,
                clientType = ClientType.POS_TERMINAL
            )
            client.post("api/v1/citacomsys/inventory/events/update-subscription") {
                setBody(request)
            }
            Napier.i { "[SSE] Dynamic subscription updated successfully (Topics: $topics, WS: $workspaceId, Branch: $branchId)" }
        }.onFailure {
            Napier.e(it) { "Failed to update SSE subscription" }
        }
    }

    private val sseJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun processEvent(event: ServerSentEvent) {
        Napier.i(tag = "SSE") { "Received SSE event: ${event.id} | ${event.event}" }

        if (event.event == "heartbeat" || event.event == "ping" || event.event == "connection_established") {
            lastHeartbeat = Clock.System.now().epochSeconds
            return
        }

        val data = event.data ?: return
        val eventName = event.event ?: "unknown"

        scope.launch {
            try {
                val decodedEvent = decodeEvent(eventName, data)
                if (decodedEvent != null) {
                    Napier.i(tag = "SSE") { "Decoded event [${decodedEvent::class.simpleName}] with ID: ${decodedEvent.id}" }

                    if (decodedEvent is EntityChangeEvent) {
                        prefs.setLastEventSeq(decodedEvent.seq)
                    }

                    // Emit to global EventBus for components & ViewModels
                    EventBus.publish(decodedEvent)

                    // Secondary security trigger if session revoked
                    if (decodedEvent is UserSessionRevokedEvent) {
                        EventBus.publish(AppLifecycleEvent.UserSessionRevoked)
                    }
                } else {
                    Napier.w(tag = "SSE") { "Unrecognized SSE event name: $eventName | Data: $data" }
                }
            } catch (e: Exception) {
                Napier.e(e) { "Failed to decode SSE event: $eventName | Data: $data" }
            }
        }
    }

    private fun decodeEvent(eventName: String, data: String): CitasEvent? {
        return runCatching {
            when (eventName) {
                EntityChangeEvent.name, "entity_change" -> sseJson.decodeFromString<EntityChangeEvent>(data)
                SubscriptionEvent.ID, "workspace.subscription.change" -> sseJson.decodeFromString<SubscriptionEvent>(data)
                UserSessionRevokedEvent.name, "USER.SESSION.REVOKED" -> sseJson.decodeFromString<UserSessionRevokedEvent>(data)
                DeviceSecurityEvent.name, "DEVICE.UPDATED" -> sseJson.decodeFromString<DeviceSecurityEvent>(data)
                WorkstationSecurityEvent.name, "SECURITY_INVALIDATION" -> sseJson.decodeFromString<WorkstationSecurityEvent>(data)
                BusinessSecurityEvent.name, "WORKSPACE.UPDATED" -> sseJson.decodeFromString<BusinessSecurityEvent>(data)
                OrgRuntimeConfig.ID, "RUNTIME.CONFIG", "ORG.CONFIG.UPDATED" -> sseJson.decodeFromString<OrgRuntimeConfig>(data)
                UserCountEvent.name, "user_count" -> sseJson.decodeFromString<UserCountEvent>(data)
                DeviceUpdatedEvent.name -> sseJson.decodeFromString<DeviceUpdatedEvent>(data)
                DeviceRevokedEvent.name -> sseJson.decodeFromString<DeviceRevokedEvent>(data)
                WorkstationBindingEvent.name -> sseJson.decodeFromString<WorkstationBindingEvent>(data)
                UserWorkspaceBindingEvent.ID -> sseJson.decodeFromString<UserWorkspaceBindingEvent>(data)
                WorkspaceEvent.ID -> sseJson.decodeFromString<WorkspaceEvent>(data)
                BillingEvent.name -> sseJson.decodeFromString<BillingEvent>(data)
                ThemeEvent.name -> sseJson.decodeFromString<ThemeEvent>(data)
                else -> runCatching { sseJson.decodeFromString<CitasEvent>(data) }.getOrNull()
            }
        }.getOrNull()
    }

    override suspend fun disconnect() {
        Napier.i { "[SSE] disconnect requested" }
        sessionJob?.cancelAndJoin()
        sessionJob = null
        _systemConnectionStatus.value = SystemEventConnectionStatus.DISCONNECTED
    }

    private fun backoffDelay(attempt: Int): Long {
        val base = 1000L
        val max = 30000L
        val multiplier = 1 shl attempt.coerceAtMost(5)
        val delay = min(base * multiplier, max)
        val jitter = (delay * 0.2 * (Random.nextDouble() * 2 - 1)).toLong()
        return (delay + jitter).coerceIn(500L, max)
    }
}
