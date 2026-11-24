package com.citacomsys.core.mesh.service

import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.mesh.model.*
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.notification.CitasNotificationType
import com.citacomsys.core.notification.InAppNotification
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.notification.system.AppNotification
import com.citacomsys.core.notification.system.AppNotificationAction
import com.citacomsys.core.notification.system.AppNotificationManager
import com.citacomsys.core.notification.system.NotificationActionRegistry
import com.citacomsys.kernel.service.PlatformRuntimeStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Clock

@OptIn(ExperimentalStdlibApi::class)
internal class MeshManagerImpl(
    private val satelliteNodeManager: SatelliteNodeManager,
    private val motherHubManager: MotherHubManager,
    private val platformStore: PlatformRuntimeStore,
    private val sessionManager: AppSessionManager,
    private val prefs: SyncPreferencesRepository,
    private val notificationService: InAppNotificationService,
    private val scope: CoroutineScope
) : MeshManager {

    override val state: StateFlow<EngineState> = motherHubManager.state

    override val meshSignals: Flow<NodeActivity> = merge(
        satelliteNodeManager.meshSignals,
        motherHubManager.meshSignals
    )
    override val activeHub: StateFlow<MeshPeerInfo?> = satelliteNodeManager.activeHub
    override val discoveredPeers: StateFlow<List<MeshPeerInfo>> = satelliteNodeManager.discoveredPeers
    override val activeSatellites: StateFlow<Map<String, SatelliteStatus>> = motherHubManager.activeSatellites
    override val connectionHistory: StateFlow<List<NodeActivity>> = motherHubManager.connectionHistory

    override val connectionState: StateFlow<MeshConnectionState> = satelliteNodeManager.connectionState
    override val boundPeer: StateFlow<MeshPeerInfo?> = satelliteNodeManager.boundPeer

    private var meshJob: Job? = null

    override fun startMesh() {
        Napier.i { "Initializing Citas Mesh Policy & Service Engine..." }

        meshJob?.cancel()
        meshJob = scope.launch {
            // Role & Service Monitor
            combine(
                platformStore.organizationConfig.map { it?.organizationId },
                prefs.isMotherHubEnabled
            ) { orgId, isHub ->
                orgId to isHub
            }.collect { (orgId, isHub) ->
                if (orgId == null) {
                    satelliteNodeManager.stopNodeService()
                    motherHubManager.demoteFromHub()
                    return@collect
                }

                if (isHub) {
                    satelliteNodeManager.stopNodeService()
                    motherHubManager.startAdvertiser()
                } else {
                    motherHubManager.demoteFromHub()
                    satelliteNodeManager.stopNodeService()
                }
            }
        }
    }

    override fun startDiscovery() {
        platformStore.organizationId?.let { orgId ->
            satelliteNodeManager.startNodeService(orgId)
        }
    }

    override fun bindPeer(peer: MeshPeerInfo) {
        satelliteNodeManager.bindPeer(peer)
    }

    override suspend fun startEngine() {
        val success = motherHubManager.startEngine()
        if (success) showSuccess("Mother Hub server engine started successfully.")
    }

    override fun onHubUnreachable() {
        val hasBoundHub = !prefs.activeHubIp.value.isNullOrBlank() || satelliteNodeManager.boundPeer.value != null
        if (hasBoundHub) {
            platformStore.organizationId?.let { orgId ->
                Napier.i(tag = "MESH_MANAGER") { "Bound Hub unreachable. Auto-triggering LAN discovery to find Hub..." }
                val actionId = "rediscover_hub_${Clock.System.now().toEpochMilliseconds()}"
                NotificationActionRegistry.register(actionId) {
                    scope.launch {
                        satelliteNodeManager.startNodeService(orgId)
                    }
                }
                AppNotificationManager.addNotification(
                    AppNotification(
                        id = "hub_unreachable",
                        title = "Mother Hub Unreachable",
                        message = "Connection lost to Mother Hub. Click to re-scan LAN network.",
                        isCritical = true,
                        actions = listOf(AppNotificationAction(actionId, "Scan LAN"))
                    )
                )
                satelliteNodeManager.startNodeService(orgId)
            }
        }
    }

    override fun stopService() {
        satelliteNodeManager.stopService()
        motherHubManager.stopService()
        stopMesh()
        showSuccess("All Mesh Services stopped cleanly.")
    }

    override fun stopMesh() {
        meshJob?.cancel()
        meshJob = null
        satelliteNodeManager.stopNodeService()
        motherHubManager.demoteFromHub()
        Napier.i { "MeshManager: Mesh services stopped." }
    }

    override suspend fun reportActivity(activity: NodeActivity) {
        if (prefs.isMotherHubEnabled.value) {
            motherHubManager.recordActivityLocally(activity)
        } else {
            satelliteNodeManager.reportActivityToHub(activity)
        }
    }

    override fun monitorNode(bindingId: String) {
        motherHubManager.pokeNodeForSync(bindingId)
    }

    override fun unregisterNode(hardwareId: String) {
        motherHubManager.unregisterNode(hardwareId)
    }

    private fun showSuccess(message: String) {
        notificationService.notify(
            InAppNotification(
                message = message,
                type = CitasNotificationType.SUCCESS
            )
        )
    }

    private fun showError(message: String?) {
        notificationService.notify(
            InAppNotification(
                message = message ?: "An unexpected error occurred",
                type = CitasNotificationType.ERROR
            )
        )
    }
}
