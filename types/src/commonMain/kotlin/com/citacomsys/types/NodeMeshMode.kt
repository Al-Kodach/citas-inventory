package com.citacomsys.types

import kotlinx.serialization.Serializable

/**
 * Defines physical network routing and mesh mode for the device.
 */
@Serializable
enum class NodeMeshMode {
    /** Direct Cloud Server Communication Mode (Standalone Node or Mother Hub). */
    NODE,

    /** Local Mother Hub LAN Relay Communication Mode. */
    SATELLITE,

    /** Hybrid Mesh Routing Mode (LAN Relay with Cloud Fallback). */
    HYBRID;

    val isSatellite: Boolean
        get() = this == SATELLITE || this == HYBRID

    val isDirectNode: Boolean
        get() = this == NODE

    val isHybrid: Boolean
        get() = this == HYBRID

    companion object {
        fun safeValueOf(value: String?): NodeMeshMode {
            if (value.isNullOrBlank()) return NODE
            return when (value.uppercase()) {
                "SATELLITE", "LAN_MESH", "LOCAL_LOCK" -> SATELLITE
                "HYBRID", "HYBRID_RELAY", "HYBRID_MESH" -> HYBRID
                "NODE", "CLOUD_ONLY" -> NODE
                else -> runCatching { valueOf(value.uppercase()) }.getOrDefault(NODE)
            }
        }
    }
}
