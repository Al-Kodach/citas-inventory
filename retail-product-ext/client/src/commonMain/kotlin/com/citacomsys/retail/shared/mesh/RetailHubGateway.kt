package com.citacomsys.retail.shared.mesh

import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.model.ActivityType
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.mesh.security.hubSyncMetadata
import com.citacomsys.retail.api.dto.RetailMetadataUpdateRequest
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.retail.shared.RETAIL_ENDPOINT
import com.citacomsys.retail.shared.data.repository.ClientRetailRepository
import com.citacomsys.types.EntityType
import com.citacomsys.types.MeshHop
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Industrial Mesh Gateway for the Retail extension.
 */
internal class RetailHubGateway(
    private val repository: ClientRetailRepository,
    private val json: Json
) : HubServiceGateway {
    override val domain: String = RETAIL_ENDPOINT
    override val entityType: EntityType = EntityType.PRODUCT

    override fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit) {
        route(RETAIL_ENDPOINT) {
            post {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val payload = call.receive<String>()
                val product = json.decodeFromString(RetailProduct.serializer(), payload)

                // If a sync request, capture all metadata
                val actorId = meta?.actorId ?: call.request.header("X-Actor-Id") ?: "mesh-node"
                val actorName = meta?.actorName ?: call.request.header("X-Actor-Name") ?: "Industrial Mesh Node"

                ActiveRoutingChainHolder.set(routingChainJson)
                try {
                    repository.createProduct(
                        product = product,
                        actorId = actorId,
                        actorName = actorName,
                    ).getOrThrow()
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }


                // Broadcast sync signal to all connected satellites
                broadcastAlert(
                    NodeActivity(
                        bindingId = "MOTHER_HUB",
                        timestamp = Clock.System.now(),
                        action = "SYNC_REQUIRED",
                        summary = "New Product added on Mother Hub",
                        type = ActivityType.SYNC_SIGNAL
                    )
                )

                call.respond(HttpStatusCode.OK)
            }

            put("{id}/metadata") {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val payload = call.receive<String>()
                val request = json.decodeFromString(RetailMetadataUpdateRequest.serializer(), payload)
                val productId = call.requirePathParameter("id")

                // If a sync request, capture all metadata
                val actorId = meta?.actorId ?: call.request.header("X-Actor-Id") ?: "mesh-node"
                val actorName = meta?.actorName ?: call.request.header("X-Actor-Id") ?: "Industrial Mesh Node"

                ActiveRoutingChainHolder.set(routingChainJson)
                try {
                    repository.updateRetailMetadata(
                        productId = productId,
                        request = request,
                        actorId = actorId,
                        actorName = actorName
                    ).getOrThrow()
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }


                // Broadcast sync signal to all connected satellites
                broadcastAlert(
                    NodeActivity(
                        bindingId = "MOTHER_HUB",
                        timestamp = Clock.System.now(),
                        action = "SYNC_REQUIRED",
                        summary = "Product data updated on Mother Hub",
                        type = ActivityType.SYNC_SIGNAL
                    )
                )

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
