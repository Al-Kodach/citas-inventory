package com.citacomsys.pharmacy.shared.mesh

import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.model.ActivityType
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.mesh.security.hubSyncMetadata
import com.citacomsys.pharmacy.api.dto.PharmacyMetadataUpdateRequest
import com.citacomsys.pharmacy.api.model.PharmacyProduct
import com.citacomsys.pharmacy.shared.PHARMACY_ENDPOINT
import com.citacomsys.pharmacy.shared.data.repository.ClientPharmacyRepository
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
 * Industrial Mesh Gateway for the Pharmacy extension.
 * Delegates local mutations to ClientPharmacyRepository, relying on telemetry audit logs for actor tracking.
 */
internal class PharmacyHubGateway(
    private val repository: ClientPharmacyRepository,
    private val json: Json
) : HubServiceGateway {
    override val domain: String = PHARMACY_ENDPOINT
    override val entityType: EntityType = EntityType.PRODUCT

    override fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit) {
        route(PHARMACY_ENDPOINT) {
            post {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val actorId = meta?.actorId ?: call.request.header("X-Actor-Id") ?: "mesh-node"
                val actorName = meta?.actorName ?: call.request.header("X-Actor-Name") ?: "Industrial Mesh Node"
                val payload = call.receive<String>()
                val product = json.decodeFromString(PharmacyProduct.serializer(), payload)

                ActiveRoutingChainHolder.set(routingChainJson)
                try {
                    repository.createProduct(
                        product = product,
                        actorId = actorId,
                        actorName = actorName
                    ).getOrThrow()
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }

                broadCastSyncAlert(broadcastAlert)
                call.respond(HttpStatusCode.OK)
            }

            put("{id}/metadata") {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val payload = call.receive<String>()
                val request = json.decodeFromString(PharmacyMetadataUpdateRequest.serializer(), payload)
                
                val actorId = meta?.actorId ?: call.request.header("X-Actor-Id") ?: "mesh-node"
                val actorName = meta?.actorName ?: call.request.header("X-Actor-Name") ?: "Industrial Mesh Node"

                ActiveRoutingChainHolder.set(routingChainJson)
                try {
                    repository.updatePharmacyMetadata(
                        productId = id,
                        request = request,
                        actorId = actorId,
                        actorName = actorName
                    ).getOrThrow()
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }

                broadCastSyncAlert(broadcastAlert)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    // Broadcast sync signal to all connected satellites
    private fun broadCastSyncAlert(broadcastAlert: (NodeActivity) -> Unit) {
        broadcastAlert(
            NodeActivity(
                bindingId = "MOTHER_HUB",
                timestamp = Clock.System.now(),
                action = "SYNC_REQUIRED",
                summary = "New product added on Mother Hub",
                type = ActivityType.SYNC_SIGNAL
            )
        )
    }
}
