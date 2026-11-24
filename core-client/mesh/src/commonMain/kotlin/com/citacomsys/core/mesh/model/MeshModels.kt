package com.citacomsys.core.mesh.model

import com.citacomsys.types.ProductType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Information broadcasted by a node during local discovery.
 */
@Serializable
data class MeshPeerInfo(
    val hardwareId: String,
    val bindingId: String,
    val organizationId: String,
    val workspaceId: String?,
    val appType: ProductType?,
    val workstationName: String,
    val ipAddress: String,
    val port: Int,
    val isMotherHub: Boolean = false,
    val orgVerificationToken: String? = null,
    val tlsFingerprint: String? = null,
    val lastSeenAt: Instant
)

/**
 * Defines the nature of a reported mesh activity.
 */
@Serializable
enum class ActivityType {
    TELEMETRY,      // Status updates
    STATE_CHANGE,   // Industrial updates (Sale, Inventory update)
    SYNC_SIGNAL     // Broadcast telling others to refresh data
}

/**
 * Activity telemetry reported by satellite nodes to the Mother Hub.
 */
@Serializable
data class NodeActivity(
    val bindingId: String,
    val timestamp: Instant,
    val action: String,
    val summary: String,
    val type: ActivityType = ActivityType.TELEMETRY,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Current status of a satellite node as seen by the Mother Hub.
 */
@Serializable
data class SatelliteStatus(
    val peerInfo: MeshPeerInfo,
    val isOnline: Boolean,
    val lastSyncAt: Instant?,
    val activeUser: String?
)

/**
 * Connection and node registration lifecycle state for Mesh Nodes & Workstations.
 */
@Serializable
enum class MeshConnectionState {
    DISCONNECTED,     // No connection or discovery active
    DISCOVERING,      // Step 1: Scanning local network for LAN peers, workstations & Mother Hubs
    BOUND,            // Step 2: Selected target workstation / Hub; showing binding info
    ENGINE_READY,     // Step 3: Binding confirmed; ready to start the engine
    ENGINE_RUNNING    // Step 3 (Active): Mesh & Data Sync Engine running in foreground/background
}
