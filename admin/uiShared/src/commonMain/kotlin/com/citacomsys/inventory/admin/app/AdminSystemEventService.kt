package com.citacomsys.inventory.admin.app

import com.citacomsys.core.domain.service.SystemEventConnectionStatus
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.shared.event.ClientType
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SseMessage
import com.citacomsys.shared.event.SseSubscriptionRequest
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.Koin
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AdminSystemEventService(
    private val client: HttpClient,
    private val scope: CoroutineScope,
    private val tokenStore: TokenStore
) : StartupTask {
    private val _connectionStatus = MutableStateFlow(SystemEventConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<SystemEventConnectionStatus> = _connectionStatus.asStateFlow()
    private var sessionJob: Job? = null
    private var lastHeartbeat: Long = 0L


    override val id: String = "core.system_event"
    override val runLevel: StartupRunLevel = StartupRunLevel.WORKSPACE_READY

    override suspend fun run(koin: Koin) {
        scope.launch {
            tokenStore.accessToken.collect {
                if (it.isNullOrBlank()) {
                    disconnect()
                } else {
                    connect()
                }
            }
        }
    }

    fun connect() {
        if (sessionJob?.isActive == true) return

        sessionJob?.cancel()
        sessionJob = scope.launch(Dispatchers.Default) {
            var attempt = 0
            while (isActive) {
                try {
                    _connectionStatus.value = SystemEventConnectionStatus.CONNECTING

                    // Admin connects subscribing specifically to TENANT_ADMIN and SECURITY_ALERT
                    client.sse(
                        urlString = "api/v1/citacomsys/inventory/events/subscribe?topics=TENANT_ADMIN,SECURITY_ALERT"
                    ) {
                        Napier.i { "[Admin SSE] Connected to admin event stream." }
                        _connectionStatus.value = SystemEventConnectionStatus.CONNECTED
                        attempt = 0
                        lastHeartbeat = Clock.System.now().epochSeconds

                        // Send explicit subscription packet identifying as ADMIN_PLATFORM
                        launch(Dispatchers.Default) {
                            runCatching {
                                val command = SseSubscriptionRequest(
                                    clientId = "admin_console",
                                    workspaceId = null,
                                    branchId = null,
                                    topics = setOf("TENANT_ADMIN", "SECURITY_ALERT"),
                                    clientType = ClientType.ADMIN_PLATFORM
                                )
                                client.post("api/v1/citacomsys/inventory/events/update-subscription") {
                                    setBody(command)
                                }
                                Napier.i { "[Admin SSE] Sent subscription packet for ADMIN_PLATFORM" }
                            }.onFailure {
                                Napier.e(it) { "Failed to send admin subscription packet" }
                            }
                        }

                        val watchdogJob = launch {
                            while (isActive) {
                                delay(15.seconds)
                                val elapsed = Clock.System.now().epochSeconds - lastHeartbeat
                                if (elapsed > 60) {
                                    Napier.w { "[Admin SSE] Stream silent for ${elapsed}s — reconnecting." }
                                    break
                                }
                            }
                        }

                        incoming.collect { event ->
                            handleAdminEvent(event)
                        }

                        watchdogJob.cancel()
                    }
                } catch (e: Exception) {
                    _connectionStatus.value = SystemEventConnectionStatus.DISCONNECTED
                    if (isActive) {
                        val delayMs = backoffDelay(attempt)
                        Napier.e(e) { "[Admin SSE] Connection error. Retrying in ${delayMs}ms..." }
                        delay(delayMs.milliseconds)
                        attempt++
                    }
                }
            }
        }
    }

    private fun handleAdminEvent(event: ServerSentEvent) {
        if (event.event == "heartbeat" || event.event == "ping" || event.event == "connection_established") {
            lastHeartbeat = Clock.System.now().epochSeconds
            return
        }

        val data = event.data ?: return
        scope.launch {
            runCatching {
                val message = Json.decodeFromString<SseMessage>(data)
                // Publish admin event onto EventBus for Admin UI / Dashboard widgets to react instantly
                EventBus.publish(message)
                Napier.i { "[Admin SSE] Received admin event: ${message.event} | Topic: ${message.topic}" }
            }.onFailure {
                Napier.e(it) { "Failed to decode admin SSE event: ${event.event}" }
            }
        }
    }

    fun disconnect() {
        scope.launch {
            sessionJob?.cancelAndJoin()
            sessionJob = null
            _connectionStatus.value = SystemEventConnectionStatus.DISCONNECTED
        }
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
