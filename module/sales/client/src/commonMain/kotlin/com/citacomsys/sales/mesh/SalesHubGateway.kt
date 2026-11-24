package com.citacomsys.sales.mesh

import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.api.sales.dto.CreateSaleRequest
import com.citacomsys.api.sales.dto.PreviewResponse
import com.citacomsys.api.sales.dto.PreviewSaleRequest
import com.citacomsys.api.sales.dto.SaleResult
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.model.ActivityType
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.mesh.security.hubSyncMetadata
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.sales.data.repository.ClientSalesRepository
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
 * Industrial Mesh Gateway for the Sales module.
 * Enables the Mother Hub to process satellite sales and queries locally via pluggable route registration.
 */
internal class SalesHubGateway(
    private val repository: ClientSalesRepository,
    private val json: Json,
    private val telemetryClient: TelemetryLogClient
) : HubServiceGateway {
    override val domain: String = "sales"
    override val entityType: EntityType = EntityType.SALE

    override fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit) {
        route("sales") {
            // Finalize / Create Sale
            post {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val workspaceId = meta?.wsId ?: call.request.headers["X-Workspace-Id"] ?: ""
                val payload = call.receive<String>()
                val request = json.decodeFromString(CreateSaleRequest.serializer(), payload)
                
                ActiveRoutingChainHolder.set(routingChainJson)
                val result = try {
                    repository.finalizeSale(request)
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }

                // Broadcast sync signal to all connected satellites
                broadcastAlert(
                    NodeActivity(
                        bindingId = "MOTHER_HUB",
                        timestamp = Clock.System.now(),
                        action = "SYNC_REQUIRED",
                        summary = "Sale finalized on Mother Hub",
                        type = ActivityType.SYNC_SIGNAL
                    )
                )

                if (result is SaleResult.Success) {
                    telemetryClient.trackApplicationEvent(
                        actorId = "mesh-node",
                        entityType = "SALE",
                        entityId = result.saleId,
                        action = "FINALIZE_SALE_HUB_RELAY",
                        summary = "Finalized sale via Hub gateway",
                        workspaceId = workspaceId
                    )
                }

                call.respondText(
                    json.encodeToString(SaleResult.serializer(), result),
                    ContentType.Application.Json
                )
            }

            // Get Sale Details
            get("{saleId}") {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val workspaceId = meta?.wsId ?: call.request.headers["X-Workspace-Id"] ?: ""
                val saleId = call.parameters["saleId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                ActiveRoutingChainHolder.set(routingChainJson)
                val details = try {
                    repository.getSaleDetails(saleId, workspaceId).getOrThrow()
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }

                call.respondText(
                    json.encodeToString(SaleDetails.serializer(), details),
                    ContentType.Application.Json
                )
            }

            // Preview Sale (Invoice)
            post("previewInvoice") {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val payload = call.receive<String>()
                val request = json.decodeFromString(PreviewSaleRequest.serializer(), payload)
                
                ActiveRoutingChainHolder.set(routingChainJson)
                val previewResponse = try {
                    repository.previewSale(request).getOrThrow()
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }

                call.respondText(
                    json.encodeToString(PreviewResponse.serializer(), previewResponse),
                    ContentType.Application.Json
                )
            }
        }
    }
}
