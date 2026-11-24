package com.citacomsys.core.mesh.service

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.mesh.gateway.HubPlugin
import com.citacomsys.core.mesh.model.ActivityType
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.model.SatelliteStatus
import com.citacomsys.core.mesh.security.MeshProtocol
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.mesh.server.LocalMeshServerEngine
import com.citacomsys.core.notification.AlertService
import com.citacomsys.database.WorkstationBindingEntity
import com.citacomsys.device.service.DeviceService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.NodeMeshMode
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Manages Mother Hub gateway operations: Netty server lifecycle,
 * LAN hub advertising, persistent SQLite workstation binding & monitoring, and sync pokes.
 */
internal class MotherHubManager(
    private val deviceService: DeviceService,
    private val platformStore: PlatformRuntimeStore,
    private val hubAdvertiser: LocalHubAdvertiser,
    private val serverEngine: LocalMeshServerEngine,
    private val prefs: SyncPreferencesRepository,
    private val activityStore: MeshActivityStore,
    private val dbManager: InventoryDatabaseManager,
    private val plugins: List<HubPlugin>,
    private val scope: CoroutineScope
) {
    private val TAG = "MOTHER_HUB_MANAGER"

    private val _hubState = MutableStateFlow<EngineState>(EngineState.Idle)
    val state: StateFlow<EngineState> = _hubState.asStateFlow()

    private val _meshSignals = MutableSharedFlow<NodeActivity>(extraBufferCapacity = 64)
    val meshSignals = _meshSignals.asSharedFlow()

    private val _activeSatellites = MutableStateFlow<Map<String, SatelliteStatus>>(emptyMap())
    val activeSatellites: StateFlow<Map<String, SatelliteStatus>> = _activeSatellites.asStateFlow()

    private val _connectionHistory = MutableStateFlow<List<NodeActivity>>(emptyList())
    val connectionHistory: StateFlow<List<NodeActivity>> = _connectionHistory.asStateFlow()

    private var watchdogJob: Job? = null

    init {
        scope.launch {
            loadConnectionHistory()
            loadRegisteredSatellitesFromDb()
            Napier.i(tag = TAG) { "MotherHubManager initialized. Persisted satellite bindings loaded." }
        }

        scope.launch {
            EventBus.subscribe<EntityChangeEvent>().onEach { event ->
                if (prefs.isMotherHubEnabled.value) {
                    val activity = NodeActivity(
                        bindingId = "MOTHER_HUB",
                        timestamp = Clock.System.now(),
                        action = "SYNC_REQUIRED",
                        summary = "Inventory/Entity change finalized on Mother Hub (${event.entityType})",
                        type = ActivityType.SYNC_SIGNAL
                    )
                    _meshSignals.emit(activity)
                    Napier.i(tag = TAG) { "Mother Hub broadcasted SYNC_REQUIRED due to local EntityChangeEvent: ${event.entityType}" }
                }
            }.launchIn(this)
        }
    }

    private suspend fun loadConnectionHistory() {
        val history = activityStore.getRecentActivities(100)
        _connectionHistory.value = history
    }

    private suspend fun loadRegisteredSatellitesFromDb() {
        runCatching {
            val myHardwareId = deviceService.deviceInfo.value.hardwareId
            val db = dbManager.getDatabase()
            val rows = db.workstationQueries.selectAll().executeAsList()
            val map = rows
                .filter { it.hardware_id != myHardwareId && it.hardware_id.isNotBlank() }
                .associate { entity ->
                    entity.hardware_id to SatelliteStatus(
                        peerInfo = MeshPeerInfo(
                            hardwareId = entity.hardware_id,
                            bindingId = entity.hardware_id,
                            organizationId = platformStore.organizationId ?: "",
                            workspaceId = entity.workspace_id.takeIf { it.isNotBlank() },
                            appType = null,
                            workstationName = entity.name ?: "Satellite Node",
                            ipAddress = "",
                            port = 0,
                            isMotherHub = false,
                            lastSeenAt = entity.last_seen_at
                        ),
                        isOnline = entity.status == "ACTIVE",
                        lastSyncAt = entity.last_seen_at,
                        activeUser = null
                    )
                }
            _activeSatellites.value = map
            Napier.i(tag = TAG) { "Restored ${map.size} registered satellite workstations from SQLite database." }
        }.onFailure { e ->
            Napier.e(tag = TAG, throwable = e) { "Failed to load registered satellite workstations from SQLite DB" }
        }
    }

    /**
     * 4-Stage Mother Hub Promotion & Activation Pipeline:
     * 1. Discover & Validate local topology & Organization ID.
     * 2. Bind hardware identity & store cryptographic binding activity in SQLite.
     * 3. Enable Mother Hub flag (`isMotherHubEnabled = true`) & Node mode.
     * 4. Start LAN mDNS advertiser (Server Engine remains inactive until startEngine() is invoked).
     */
    fun promoteToHub(): Boolean {
        val organizationId = platformStore.organizationId
        if (organizationId.isNullOrBlank()) {
            Napier.w(tag = TAG) { "Cannot promote to Mother Hub: Organization ID not found." }
            AlertService.showError("Promotion denied: No active organization found.")
            return false
        }

        val hasWorkspaces = (platformStore.organizationConfig.value?.workspaces?.size ?: 0) > 0
        if (!hasWorkspaces) {
            Napier.w(tag = TAG) { "Cannot promote to Mother Hub: Organization has no active workspace." }
            AlertService.showError("Promotion denied: Mother Hub deployment requires an active workspace.")
            return false
        }

        val hardwareId = deviceService.deviceInfo.value.hardwareId
        val displayName = deviceService.deviceInfo.value.displayName

        if (hardwareId.isBlank()) {
            Napier.w(tag = TAG) { "Cannot promote to Mother Hub: Hardware ID not bound." }
            AlertService.showError("Promotion denied: Workstation hardware ID not bound.")
            return false
        }

        val bindActivity = NodeActivity(
            bindingId = hardwareId,
            timestamp = Clock.System.now(),
            action = "HUB_BINDING",
            summary = "Promoted workstation '$displayName' ($hardwareId) as Mother Hub"
        )

        scope.launch {
            activityStore.saveActivity(bindActivity)
            loadConnectionHistory()
        }

        prefs.setMotherHubEnabled(true)
        prefs.setMeshSyncMode(NodeMeshMode.NODE)

        startAdvertiser()
        return true
    }

    fun startAdvertiser() {
        val organizationId = platformStore.organizationId ?: return
        val hardwareId = deviceService.deviceInfo.value.hardwareId
        val displayName = deviceService.deviceInfo.value.displayName

        hubAdvertiser.start(
            identity = HubIdentity(
                hubId = hardwareId,
                hubName = displayName,
                ipAddress = "",
                port = MeshProtocol.TLS_PORT,
                tlsFingerprint = serverEngine.tlsFingerprint,
                lastSeenAt = Clock.System.now()
            ),
            secret = organizationId.encodeToByteArray()
        )
        Napier.i(tag = TAG) { "Mother Hub advertiser started for Org $organizationId" }
    }

    fun startEngine(): Boolean {
        if (_hubState.value == EngineState.Running) return true

        val organizationId = platformStore.organizationId
        if (organizationId.isNullOrBlank()) {
            AlertService.showError("Cannot start engine: No active organization found.")
            return false
        }

        val hasWorkspaces = (platformStore.organizationConfig.value?.workspaces?.size ?: 0) > 0
        if (!hasWorkspaces) {
            AlertService.showError("Cannot start engine: Requires an active workspace.")
            return false
        }

        startServer()
        startHeartbeatWatchdog()
        startAdvertiser()
        startInactivityAutoKillTimer()
        AlertService.showSuccess("Mother Hub server engine started successfully!")
        return true
    }

    fun demoteFromHub() {
        Napier.i(tag = TAG) { "Demoting node from Mother Hub." }
        prefs.setMotherHubEnabled(false)
        stopServer()
        stopHeartbeatWatchdog()
        hubAdvertiser.stop()
    }

    fun stopService() {
        Napier.i(tag = TAG) { "Stopping all Mother Hub gateway services..." }
        demoteFromHub()
        AlertService.showInfo("Mother Hub Mesh Service stopped cleanly.")
    }

    private fun startServer() {
        try {
            serverEngine.start(
                port = MeshProtocol.TLS_PORT,
                plugins = plugins,
                broadcastAlert = { activity ->
                    scope.launch {
                        recordActivityLocally(activity)
                        activityStore.saveActivity(activity)
                        loadConnectionHistory()
                        _meshSignals.emit(activity)
                    }
                },
                meshSignals = meshSignals
            )
            _hubState.value = EngineState.Running
            Napier.i(tag = TAG) { "Mother Hub local server started on port ${MeshProtocol.TLS_PORT}" }
        } catch (e: Exception) {
            _hubState.value = EngineState.Stopped
            Napier.e(tag = TAG, throwable = e) { "Failed to start Mother Hub local server" }
        }
    }

    private fun stopServer() {
        try {
            serverEngine.stop()
        } catch (_: Exception) {}
        _hubState.value = EngineState.Stopped
    }

    private val DEAD_NODE_TTL_DAYS = 3

    private fun startHeartbeatWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(15000.milliseconds) // Check every 15 seconds
                val now = Clock.System.now()
                val current = _activeSatellites.value.toMutableMap()
                var updated = false
                val deadNodeIdsToRemove = mutableListOf<String>()

                current.forEach { (id, sat) ->
                    val lastSync = sat.lastSyncAt
                    if (lastSync != null) {
                        val inactiveDays = (now - lastSync).inWholeDays
                        val inactiveSeconds = (now - lastSync).inWholeSeconds

                        // Mark node OFFLINE if inactive for > 60 seconds
                        if (sat.isOnline && inactiveSeconds > 60) {
                            current[id] = sat.copy(isOnline = false)
                            updated = true

                            runCatching {
                                val db = dbManager.getDatabase()
                                val existing = db.workstationQueries.selectById(id).executeAsOneOrNull()
                                if (existing != null) {
                                    db.workstationQueries.insertOrUpdate(
                                        existing.copy(status = "OFFLINE", last_seen_at = now)
                                    )
                                }
                            }
                            Napier.w(tag = TAG) { "Satellite node $id marked OFFLINE due to 60s inactivity timeout." }
                        }

                        // AUTO-CLEANUP: Remove node from registry & SQLite if dead for >= 3 days
                        if (inactiveDays >= DEAD_NODE_TTL_DAYS) {
                            deadNodeIdsToRemove.add(id)
                        }
                    }
                }

                if (updated) {
                    _activeSatellites.value = current
                }

                // Execute auto-cleanup for dead nodes
                deadNodeIdsToRemove.forEach { deadId ->
                    Napier.i(tag = TAG) { "Auto-cleaning dead satellite node $deadId (inactive >= $DEAD_NODE_TTL_DAYS days) from SQLite database." }
                    unregisterNode(deadId)
                }
            }
        }
    }

    private fun stopHeartbeatWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    private var inactivityJob: Job? = null
    private var lastNodeActivityTimestamp: Long = Clock.System.now().toEpochMilliseconds()

    private fun startInactivityAutoKillTimer() {
        inactivityJob?.cancel()
        lastNodeActivityTimestamp = Clock.System.now().toEpochMilliseconds()
        inactivityJob = scope.launch {
            while (isActive && _hubState.value == EngineState.Running) {
                delay(1.minutes)
                val now = Clock.System.now().toEpochMilliseconds()
                val idleMs = now - lastNodeActivityTimestamp
                val activeOnlineNodes = _activeSatellites.value.values.count { it.isOnline }

                if (activeOnlineNodes > 0) {
                    // Layer 1: Connected Node Traffic Cutoff (5 minutes of zero traffic)
                    if (idleMs >= 5.minutes.inWholeMilliseconds) {
                        Napier.w(tag = TAG) { "Connected satellite nodes idle for 5 minutes with no traffic. Auto-stopping Mother Hub server engine." }
                        demoteFromHub()
                        AlertService.showWarning("Mother Hub engine auto-stopped due to 5 minutes traffic inactivity.")
                        break
                    }
                } else {
                    // Layer 2: No Satellite Connected Cutoff (30 minutes)
                    if (idleMs >= 30.minutes.inWholeMilliseconds) {
                        Napier.w(tag = TAG) { "No satellite node connected for 30 minutes. Auto-stopping Mother Hub server engine." }
                        demoteFromHub()
                        AlertService.showWarning("Mother Hub engine auto-stopped due to 30 minutes inactivity.")
                        break
                    }
                }
            }
        }
    }

    private fun updateActivityTimestamp() {
        lastNodeActivityTimestamp = Clock.System.now().toEpochMilliseconds()
    }

    fun recordActivityLocally(activity: NodeActivity) {
        updateActivityTimestamp()
        val myHardwareId = deviceService.deviceInfo.value.hardwareId
        if (activity.bindingId == myHardwareId || activity.bindingId.isBlank()) {
            return
        }
        val current = _activeSatellites.value.toMutableMap()
        val existing = current[activity.bindingId]

        val status = existing ?: SatelliteStatus(
            peerInfo = MeshPeerInfo(
                hardwareId = activity.bindingId,
                bindingId = activity.bindingId,
                organizationId = platformStore.organizationId ?: "",
                workspaceId = null,
                appType = null,
                workstationName = "Satellite Node",
                ipAddress = "",
                port = 0,
                isMotherHub = false,
                lastSeenAt = Clock.System.now()
            ),
            isOnline = true,
            lastSyncAt = activity.timestamp,
            activeUser = null
        )

        val wasOffline = existing == null || !existing.isOnline
        val updatedStatus = status.copy(
            isOnline = true,
            lastSyncAt = activity.timestamp
        )
        current[activity.bindingId] = updatedStatus
        _activeSatellites.value = current

        // Persist satellite binding in SQLite
        val now = activity.timestamp
        scope.launch {
            runCatching {
                val db = dbManager.getDatabase()
                val bindingEntity = WorkstationBindingEntity(
                    hardware_id = activity.bindingId,
                    workspace_id = updatedStatus.peerInfo.workspaceId ?: "",
                    public_key = "",
                    fingerprint = updatedStatus.peerInfo.tlsFingerprint ?: "",
                    name = updatedStatus.peerInfo.workstationName,
                    status = "ACTIVE",
                    last_seen_at = now,
                    created_at = now
                )
                db.workstationQueries.insertOrUpdate(bindingEntity)
            }
        }

        if (wasOffline && activity.action != "SYNC_POKE") {
            pokeNodeForSync(activity.bindingId, "Auto Sync Poke on Node Reconnection")
        }
    }

    fun pokeNodeForSync(bindingId: String, actionSummary: String = "Manual Sync Poke from Hub") {
        scope.launch {
            val pokeActivity = NodeActivity(
                bindingId = bindingId,
                timestamp = Clock.System.now(),
                action = "SYNC_POKE",
                summary = actionSummary
            )
            recordActivityLocally(pokeActivity)
            activityStore.saveActivity(pokeActivity)
            loadConnectionHistory()
            _meshSignals.emit(pokeActivity)
            Napier.i(tag = TAG) { "Broadcasted Sync Poke signal to satellite $bindingId" }
        }
    }

    fun unregisterNode(hardwareId: String) {
        scope.launch {
            runCatching {
                val db = dbManager.getDatabase()
                db.workstationQueries.deleteById(hardwareId)
                val current = _activeSatellites.value.toMutableMap()
                current.remove(hardwareId)
                _activeSatellites.value = current

                val unbindActivity = NodeActivity(
                    bindingId = hardwareId,
                    timestamp = Clock.System.now(),
                    action = "NODE_REMOVED",
                    summary = "Workstation '$hardwareId' removed from Mother Hub registry"
                )
                activityStore.saveActivity(unbindActivity)
                loadConnectionHistory()
                Napier.i(tag = TAG) { "Removed satellite workstation $hardwareId from SQLite registry." }
                AlertService.showSuccess("Workstation removed from Mother Hub registry.")
            }.onFailure { e ->
                Napier.e(tag = TAG, throwable = e) { "Failed to remove satellite node $hardwareId from SQLite registry" }
                AlertService.showError("Failed to remove workstation from registry.")
            }
        }
    }
}
