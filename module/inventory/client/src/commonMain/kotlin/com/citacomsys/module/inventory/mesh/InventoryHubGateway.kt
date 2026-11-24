package com.citacomsys.module.inventory.mesh

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.model.ActivityType
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.mesh.security.hubSyncMetadata
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.module.inventory.domain.repository.ClientStockBatchRepository
import com.citacomsys.module.inventory.request.*
import com.citacomsys.types.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlin.time.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.time.Clock

/**
 * Industrial Mesh Gateway for the Inventory module.
 * Enables the Mother Hub to provide sync endpoints and local processing via pluggable route registration.
 */
internal class InventoryHubGateway(
    private val repository: ClientStockBatchRepository,
    private val dbManager: InventoryDatabaseManager,
    private val json: Json,
    private val telemetryClient: TelemetryLogClient
) : HubServiceGateway {
    override val domain: String = "inventory"
    override val entityType: EntityType = EntityType.STOCK_BATCH

    override fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit) {
        route("inventory") {

            // Stock Sync - Returns ChangeListResponse<BatchItem, String>
            get("stocks/pull") {
                val workspaceId = call.request.headers["X-Workspace-Id"] ?: ""
                val since = call.parameters["since"]?.let { Instant.parse(it) }
                    ?: Instant.fromEpochMilliseconds(0)
                val branchId = call.parameters["branchId"]

                val db = dbManager.getDatabase()
                val snapshotItems = db.batchSnapshotQueries.getLiveChangeList(
                    workspaceId = workspaceId,
                    branchId = branchId,
                    since = since
                ).executeAsList()

                val changes = snapshotItems.map { it ->
                    BatchItem(
                        batchId = it.batch_id,
                        productId = it.product_id,
                        workspaceId = it.workspace_id,
                        branchId = it.branch_id,
                        batchNumber = it.batch_number,
                        availableQuantity = it.available_quantity.toInt(),
                        sellingPriceAmountMinor = it.selling_price_amount_minor,
                        sellingPriceCurrency = it.selling_price_currency,
                        expiryDate = it.expiry_date,
                        versionTimestamp = it.version_timestamp
                    )
                }

                val response = ChangeListResponse(
                    updated = changes,
                    deleted = emptyList<String>()
                )

                val serializer = ChangeListResponse.serializer(
                    BatchItem.serializer(),
                    String.serializer()
                )
                call.respondText(
                    json.encodeToString(serializer, response),
                    ContentType.Application.Json
                )
            }

            // Restock
            post("stocks/restock") {
                val meta = call.hubSyncMetadata
                val workspaceId = meta?.wsId ?: call.request.headers["X-Workspace-Id"] ?: ""
                val actorId = meta?.actorId ?: call.request.header("X-Actor-Id") ?: "mesh-node"
                val actorName = meta?.actorName ?: call.request.header("X-Actor-Name") ?: "Industrial Mesh Node"

                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val payload = call.receive<String>()
                val request = json.decodeFromString(RestockRequest.serializer(), payload)

                ActiveRoutingChainHolder.set(routingChainJson)
                try {
                    repository.restock(request, actorId, actorName).getOrThrow()
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }

                broadCastSyncAlert(broadcastAlert)

                telemetryClient.trackApplicationEvent(
                    actorId = actorId,
                    entityType = "STOCK_BATCH",
                    entityId = request.id,
                    action = "RESTOCK_HUB_RELAY",
                    summary = "Restocked product ${request.productId} via Hub gateway",
                    workspaceId = workspaceId
                )

                call.respond(HttpStatusCode.OK)
            }

            // Stock Adjustment
            put("stocks/adjust") {
                val meta = call.hubSyncMetadata
                val workspaceId = meta?.wsId ?: call.request.headers["X-Workspace-Id"] ?: ""
                val actorId = meta?.actorId ?: call.request.header("X-Actor-Id") ?: "mesh-node"
                val actorName = meta?.actorName ?: call.request.header("X-Actor-Name") ?: "Industrial Mesh Node"

                val payload = call.receive<String>()
                val request = json.decodeFromString(AdjustStockRequest.serializer(), payload)
                repository.adjustStock(request, actorId, actorName).getOrThrow()
                broadCastSyncAlert(broadcastAlert)

                telemetryClient.trackApplicationEvent(
                    actorId = actorId,
                    entityType = "STOCK_BATCH",
                    entityId = request.batchId,
                    action = "ADJUST_HUB_RELAY",
                    summary = "Adjusted stock for batch ${request.batchId} via Hub gateway",
                    workspaceId = workspaceId
                )

                call.respond(HttpStatusCode.OK)
            }

            // Price Change
            put("stocks/{id}/price") {
                val meta = call.hubSyncMetadata
                val workspaceId = meta?.wsId ?: call.request.headers["X-Workspace-Id"] ?: ""
                val actorId = meta?.actorId ?: call.request.header("X-Actor-Id") ?: "mesh-node"
                val actorName = meta?.actorName ?: call.request.header("X-Actor-Name") ?: "Industrial Mesh Node"

                val payload = call.receive<String>()
                val request = json.decodeFromString(PriceChangeRequest.serializer(), payload)
                repository.updatePrice(request, actorId, actorName).getOrThrow()
                broadCastSyncAlert(broadcastAlert)

                telemetryClient.trackApplicationEvent(
                    actorId = actorId,
                    entityType = "STOCK_BATCH",
                    entityId = request.batchId,
                    action = "PRICE_HUB_RELAY",
                    summary = "Updated price for batch ${request.batchId} via Hub gateway",
                    workspaceId = workspaceId
                )

                call.respond(HttpStatusCode.OK)
            }

            // Multi-Workspace / Branch Map Pull (Resourceful endpoint: products/deltas)
            post("products/deltas") {
                val payload = call.receive<String>()
                val request = json.decodeFromString(MultiTargetDeltaRequest.serializer(), payload)
                val db = dbManager.getDatabase()

                val workspaceDeltas = mutableMapOf<String, WorkspaceDeltaResponse>()

                for ((workspaceId, branchCursors) in request.workspaces) {
                    if (branchCursors.isEmpty()) continue

                    val branchBatches = mutableMapOf<String, ChangeListResponse<BatchItem, String>>()

                    for (cursor in branchCursors) {
                        val batchSyncSince = cursor.lastBatchSyncAt ?: Instant.fromEpochMilliseconds(0)
                        val snapshotItems = db.batchSnapshotQueries.getLiveChangeList(
                            workspaceId = workspaceId,
                            branchId = cursor.branchId,
                            since = batchSyncSince
                        ).executeAsList()

                        val batchItems = snapshotItems.map { it ->
                            BatchItem(
                                batchId = it.batch_id,
                                productId = it.product_id,
                                workspaceId = it.workspace_id,
                                branchId = it.branch_id,
                                batchNumber = it.batch_number,
                                availableQuantity = it.available_quantity.toInt(),
                                sellingPriceAmountMinor = it.selling_price_amount_minor,
                                sellingPriceCurrency = it.selling_price_currency,
                                expiryDate = it.expiry_date,
                                versionTimestamp = it.version_timestamp
                            )
                        }

                        branchBatches[cursor.branchId] = ChangeListResponse(
                            updated = batchItems,
                            deleted = emptyList()
                        )
                    }

                    workspaceDeltas[workspaceId] = WorkspaceDeltaResponse(
                        products = ChangeListResponse(emptyList(), emptyList()),
                        branchBatches = branchBatches
                    )
                }

                val response = MultiTargetDeltaResponse(workspaceDeltas)
                call.respondText(
                    json.encodeToString(MultiTargetDeltaResponse.serializer(), response),
                    ContentType.Application.Json
                )
            }
        }
    }

    // Broadcast sync signal to all connected satellites
    private fun broadCastSyncAlert(logActivity: (NodeActivity) -> Unit) {
        logActivity(
            NodeActivity(
                bindingId = "MOTHER_HUB",
                timestamp = Clock.System.now(),
                action = "SYNC_REQUIRED",
                summary = "Inventory operation finalized on Mother Hub",
                type = ActivityType.SYNC_SIGNAL
            )
        )
    }
}
