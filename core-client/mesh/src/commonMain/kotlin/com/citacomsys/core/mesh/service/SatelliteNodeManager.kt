package com.citacomsys.core.mesh.service

import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.service.DataSyncTrigger
import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.mesh.model.MeshConnectionState
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.security.MeshCryptoManager
import com.citacomsys.core.mesh.security.MeshProtocol
import com.citacomsys.core.notification.system.AppNotification
import com.citacomsys.core.notification.system.AppNotificationAction
import com.citacomsys.core.notification.system.AppNotificationManager
import com.citacomsys.core.notification.system.NotificationActionRegistry
import com.citacomsys.device.service.DeviceService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.NodeMeshMode
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages Satellite Node mesh presence, discovery of Mother Hub, SSE signal stream reception,
 * and auto-sync triggering upon Mobile Hub re-entry or network reconnection.
 */
internal class SatelliteNodeManager(
    private val deviceService: DeviceService,
    private val platformStore: PlatformRuntimeStore,
    private val discoveryService: DiscoveryService,
    private val httpClient: HttpClient,
    private val prefs: SyncPreferencesRepository,
    private val connectivityService: NetworkConnectivityService,
    private val syncTrigger: DataSyncTrigger,
    private val scope: CoroutineScope
) {
    private val TAG = "SATELLITE_NODE_MANAGER"

    private val _meshSignals = MutableSharedFlow<NodeActivity>(extraBufferCapacity = 64)
    val meshSignals = _meshSignals.asSharedFlow()

    private val _rawPeers = MutableStateFlow<List<MeshPeerInfo>>(emptyList())
    val discoveredPeers: StateFlow<List<MeshPeerInfo>> = _rawPeers
        .map { peers ->
            val orgConfig = platformStore.organizationConfig.value ?: return@map emptyList<MeshPeerInfo>()
            val localHardwareId = deviceService.deviceInfo.value.hardwareId
            peers.filter { peer ->
                // Filter out self-discovery! A node MUST NEVER discover itself as a remote Hub peer!
                val isSelf = peer.hardwareId == localHardwareId ||
                        (peer.isMotherHub && prefs.isMotherHubEnabled.value) ||
                        peer.ipAddress == "127.0.0.1"
                if (isSelf) return@filter false

                peer.orgVerificationToken?.let { token ->
                    MeshCryptoManager.verifyDiscoveryToken(
                        token,
                        orgConfig.organizationId,
                        orgConfig.fleetDna.encodeToByteArray()
                    )
                } ?: false
            }
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    val activeHub: StateFlow<MeshPeerInfo?> = combine(
        discoveredPeers,
        prefs.activeHubIp
    ) { peers, cachedIp ->
        if (!cachedIp.isNullOrBlank()) {
            val liveHub = peers.find { it.ipAddress == cachedIp || it.isMotherHub }
            liveHub ?: MeshPeerInfo(
                hardwareId = "cached-hub",
                bindingId = "cached-hub",
                organizationId = prefs.activeHubOrgId.value ?: "",
                workspaceId = null,
                appType = null,
                workstationName = "Cached Hub ($cachedIp)",
                ipAddress = cachedIp,
                port = prefs.activeHubPort.value,
                isMotherHub = true,
                orgVerificationToken = prefs.activeHubOrgToken.value,
                tlsFingerprint = prefs.activeHubTlsFingerprint.value,
                lastSeenAt = Clock.System.now()
            )
        } else {
            peers.find { it.isMotherHub }
        }
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(),
        null
    )

    private val _boundPeer = MutableStateFlow<MeshPeerInfo?>(null)
    val boundPeer: StateFlow<MeshPeerInfo?> = _boundPeer.asStateFlow()

    private val _connectionState = MutableStateFlow(MeshConnectionState.DISCONNECTED)
    val connectionState: StateFlow<MeshConnectionState> = _connectionState.asStateFlow()

    private var sseListenerJob: Job? = null
    private var discoveryJob: Job? = null

    init {
        observeActiveHubAndConnect()
    }

    private fun observeActiveHubAndConnect() {
        scope.launch {
            var previousHubIp: String? = null

            combine(
                activeHub,
                prefs.meshSyncMode,
                connectivityService.connectionStatusFlow
            ) { hub, mode, isOnline ->
                Triple(hub, mode, isOnline)
            }.collect { (hub, mode, isOnline) ->
                val hasBoundHub = !prefs.activeHubIp.value.isNullOrBlank() || _boundPeer.value != null
                if (hub != null && hasBoundHub) {
                    if (_boundPeer.value == null) {
                        _boundPeer.value = hub
                        if (_connectionState.value == MeshConnectionState.DISCONNECTED) {
                            _connectionState.value = MeshConnectionState.BOUND
                        }
                    }

                    prefs.setActiveHubInfo(
                        ip = hub.ipAddress,
                        port = hub.port,
                        orgId = hub.organizationId,
                        token = hub.orgVerificationToken,
                        tlsFingerprint = hub.tlsFingerprint
                    )

                    // If live Hub is connected, stop active discovery scan loop
                    if (discoveryJob != null && hub.hardwareId != "cached-hub") {
                        Napier.i(tag = TAG) { "Live Mother Hub reached at ${hub.ipAddress}:${hub.port}. Stopping discovery scan." }
                        discoveryJob?.cancel()
                        discoveryJob = null
                        discoveryService.stopDiscovery()
                    }

                    // Detect Mobile Hub Re-Entry or Re-Connection -> Auto-Trigger Outbox Catch-up Sync
                    if (hub.ipAddress != previousHubIp && isOnline) {
                        Napier.i(tag = TAG) { "Mother Hub re-entry detected at ${hub.ipAddress}. Auto-triggering outbox sync..." }
                        scope.launch {
                            runCatching { syncTrigger.syncAll() }
                        }
                    }
                    previousHubIp = hub.ipAddress

                    if (mode != NodeMeshMode.NODE && !prefs.isMotherHubEnabled.value && isOnline) {
                        startSignalStreamListener(hub)
                    } else {
                        stopSignalStreamListener()
                    }
                } else {
                    stopSignalStreamListener()
                }
            }
        }
    }

    fun bindPeer(peer: MeshPeerInfo) {
        _boundPeer.value = peer
        prefs.setActiveHubInfo(
            ip = peer.ipAddress,
            port = peer.port,
            orgId = peer.organizationId,
            token = peer.orgVerificationToken,
            tlsFingerprint = peer.tlsFingerprint
        )
        _connectionState.value = MeshConnectionState.BOUND
        Napier.i(tag = TAG) { "Bound peer selected: ${peer.workstationName} [${peer.ipAddress}:${peer.port}]" }

        // Send satellite node identity payload to Mother Hub for registration in SQLite
        scope.launch {
            reportActivityToHub(
                NodeActivity(
                    bindingId = deviceService.deviceInfo.value.hardwareId,
                    timestamp = Clock.System.now(),
                    action = "NODE_JOINED",
                    summary = "Satellite Node '${deviceService.deviceInfo.value.displayName}' connected & joined Mother Hub",
                    metadata = mapOf(
                        "workstationName" to deviceService.deviceInfo.value.displayName,
                        "hardwareId" to deviceService.deviceInfo.value.hardwareId,
                        "organizationId" to (platformStore.organizationId ?: "")
                    )
                )
            )
        }
    }

    fun startNodeService(organizationId: String) {
        Napier.i(tag = TAG) { "Starting Satellite Node presence beacon and discovery for org $organizationId" }
        stopNodeService()
        _connectionState.value = MeshConnectionState.DISCOVERING

        discoveryJob = scope.launch {
            launch {
                discoveryService.startDiscovery(organizationId).collect { peers ->
                    _rawPeers.value = peers
                }
            }
            launch { broadcastPresenceBeacon(organizationId) }
        }
    }

    fun stopNodeService() {
        // No Force logs
        if (discoveryJob == null) return

        discoveryJob?.cancel()
        discoveryJob = null
        discoveryService.stopDiscovery()
        stopSignalStreamListener()
        if (_connectionState.value == MeshConnectionState.DISCOVERING) {
            _connectionState.value = MeshConnectionState.DISCONNECTED
        }
        Napier.i(tag = TAG) { "Satellite Node presence beacon and discovery stopped." }
    }

    fun stopService() {
        stopNodeService()
        _boundPeer.value = null
        _connectionState.value = MeshConnectionState.DISCONNECTED
        Napier.i(tag = TAG) { "Satellite Node mesh service stopped cleanly." }
    }

    private fun broadcastPresenceBeacon(organizationId: String) {
        val device = deviceService.deviceInfo.value
        val verificationToken = MeshCryptoManager.generateDiscoveryToken(
            organizationId = organizationId,
            secret = organizationId.encodeToByteArray()
        )

        val info = MeshPeerInfo(
            hardwareId = device.hardwareId,
            bindingId = "",
            organizationId = organizationId,
            workspaceId = null,
            appType = null,
            workstationName = device.displayName,
            ipAddress = "",
            port = MeshProtocol.TLS_PORT,
            isMotherHub = false,
            orgVerificationToken = verificationToken,
            tlsFingerprint = null,
            lastSeenAt = Clock.System.now()
        )
        discoveryService.broadcastPresence(info)
    }

    private var sseFailureCount = 0
    private val MAX_SSE_RETRY_ATTEMPTS = 5

    private fun startSignalStreamListener(hub: MeshPeerInfo) {
        sseListenerJob?.cancel()
        sseListenerJob = scope.launch {
            try {
                Napier.i(tag = TAG) { "Satellite Node connecting to Mother Hub signal stream [${hub.ipAddress}:${hub.port}]..." }
                httpClient.sse(
                    scheme = "https",
                    host = hub.ipAddress,
                    port = hub.port,
                    path = "/api/v1/mesh/signals"
                ) {
                    sseFailureCount = 0
                    incoming.collect { event ->
                        if (event.event == "mesh_signal") {
                            val activity = Json.decodeFromString<NodeActivity>(event.data ?: "")
                            Napier.d(tag = TAG) { "Received Sync Signal from Hub: ${activity.action}" }
                            _meshSignals.emit(activity)
                            // Trigger sync on receiving SYNC_POKE
                            if (activity.action == "SYNC_POKE") {
                                scope.launch {
                                    runCatching { syncTrigger.syncAll() }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                sseFailureCount++
                if (sseFailureCount <= MAX_SSE_RETRY_ATTEMPTS) {
                    Napier.w(tag = TAG) { "Satellite lost connection to Mother Hub (Attempt $sseFailureCount/$MAX_SSE_RETRY_ATTEMPTS). Retrying in 10s..." }
                    delay(10000.milliseconds)
                    startSignalStreamListener(hub)
                } else {
                    Napier.w(tag = TAG) { "Mother Hub unreachable after $MAX_SSE_RETRY_ATTEMPTS attempts. Halting active retries to conserve system resources." }
                    stopSignalStreamListener()
                    _connectionState.value = MeshConnectionState.DISCONNECTED
                    sseFailureCount = 0

                    // Dispatch actionable background notification event for satellite hub loss (Idempotent update)
                    platformStore.organizationId?.let { orgId ->
                        val actionId = "satellite_scan_action"
                        NotificationActionRegistry.register(actionId) {
                            scope.launch {
                                startNodeService(orgId)
                            }
                        }
                        AppNotificationManager.addNotification(
                            AppNotification(
                                id = "satellite_hub_offline",
                                title = "Mother Hub Offline",
                                message = "Lost persistent connection to Mother Hub after $MAX_SSE_RETRY_ATTEMPTS attempts.",
                                isCritical = true,
                                actions = listOf(AppNotificationAction(actionId, "Scan Network"))
                            )
                        )
                    }
                }
            }
        }
    }

    private fun stopSignalStreamListener() {
        sseListenerJob?.cancel()
        sseListenerJob = null
    }

    suspend fun reportActivityToHub(activity: NodeActivity) {
        val hub = activeHub.value ?: _boundPeer.value ?: return
        try {
            httpClient.post("https://${hub.ipAddress}:${hub.port}/api/v1/mesh/activity") {
                contentType(ContentType.Application.Json)
                setBody(activity)
            }
            Napier.d(tag = TAG) { "Reported satellite activity to Hub: ${activity.action}" }
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Failed to report activity to Hub" }
        }
    }
}
