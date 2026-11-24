package com.citacomsys.core.mesh.service

import com.citacomsys.core.mesh.model.MeshConnectionState
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.model.SatelliteStatus
import com.citacomsys.core.mesh.server.EngineState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Orchestrates the local mesh network for workstations and Mother Hubs.
 */
interface MeshManager {
    /**
     * Outgoing signals from this node to the mesh.
     */
    val meshSignals: Flow<NodeActivity>

    /**
     * Current state of the server engine.
     */
    val state: StateFlow<EngineState>

    /**
     * The active Mother Hub or workstation identity if discovered.
     */
    val activeHub: StateFlow<MeshPeerInfo?>

    /**
     * List of discovered peers/workstations on the LAN.
     */
    val discoveredPeers: StateFlow<List<MeshPeerInfo>>

    /**
     * For Mother Hub: Tracked status of all connected satellite nodes.
     */
    val activeSatellites: StateFlow<Map<String, SatelliteStatus>>

    /**
     * Activity audit log history for mesh nodes.
     */
    val connectionHistory: StateFlow<List<NodeActivity>>

    /**
     * Current connection & binding lifecycle state.
     */
    val connectionState: StateFlow<MeshConnectionState>

    /**
     * Currently bound peer / workstation.
     */
    val boundPeer: StateFlow<MeshPeerInfo?>

    /**
     * Starts the discovery process and joins the organizational mesh.
     */
    fun startMesh()

    /**
     * Step 1: Scans the LAN for workstations and hubs.
     */
    fun startDiscovery()

    /**
     * Step 2: Binds a target workstation or hub for connection.
     */
    fun bindPeer(peer: MeshPeerInfo)

    /**
     * Step 3: Explicitly starts the sync / mesh engine after binding confirmation.
     */
    suspend fun startEngine()

    /**
     * Satellite operation: Report local activity to the Mother Hub.
     */
    suspend fun reportActivity(activity: NodeActivity)

    /**
     * Triggered when a LAN request to the Mother Hub fails.
     */
    fun onHubUnreachable()

    /**
     * Watchdog logic to mark satellites as offline if heartbeat fails.
     */
    fun monitorNode(bindingId: String)

    /**
     * Removes a satellite workstation from the local Mother Hub SQLite registry.
     */
    fun unregisterNode(hardwareId: String)

    /**
     * Completely shuts down all mesh background services, servers, and discovery loops.
     */
    fun stopService()

    fun stopMesh()
}
