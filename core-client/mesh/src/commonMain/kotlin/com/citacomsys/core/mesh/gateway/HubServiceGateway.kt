package com.citacomsys.core.mesh.gateway

import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductType
import io.ktor.server.routing.Route

/**
 * Interface for industrial features to provide pluggable local processing routes
 * when the node is acting as a Mother Hub.
 */
interface HubServiceGateway {
    /**
     * Unique identifier for the industrial domain (e.g., "inventory", "sales").
     */
    val domain: String
    val entityType: EntityType? get() = null
    val productType: ProductType? get() = null

    /**
     * Registers Ktor server routes for this gateway on the Mother Hub.
     */
    fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit)
}
