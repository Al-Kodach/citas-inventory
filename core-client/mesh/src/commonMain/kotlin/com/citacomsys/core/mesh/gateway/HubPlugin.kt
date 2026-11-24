package com.citacomsys.core.mesh.gateway

/**
 * Interface for industrial services (Pharmacy, Retail) to contribute routes
 * to the Mother Hub's local gateway server.
 */
interface HubPlugin {
    val id: String
    fun register(registry: Any) // Platform engines will cast this to Routing
}