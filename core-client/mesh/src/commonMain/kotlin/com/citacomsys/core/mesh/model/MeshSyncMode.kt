package com.citacomsys.core.mesh.model

import kotlinx.serialization.Serializable

/**
 * Defines how a workstation communicates with the Citas Cloud.
 */
@Serializable
enum class MeshSyncMode {
    /**
     * Standard operation: Directly connect to the Cloud Kernel. 
     * No local mesh features are used.
     */
    CLOUD_ONLY,

    /**
     * Efficient operation: Attempt to find a local Mother Hub on the network
     * and use it as a gateway. Fallback to Cloud if Hub is unreachable.
     */
    HYBRID_MESH,

    /**
     * High Security/Low Bandwidth: Strictly connect via a local Mother Hub.
     * Denies all direct internet requests.
     */
    LOCAL_LOCK
}
