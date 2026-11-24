package com.citacomsys.client.customer.mesh

import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.mesh.security.hubSyncMetadata
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.module.api.customer.Customer
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.module.api.customer.CustomerLedgerEntry
import com.citacomsys.module.api.customer.CustomerLedgerRequest
import com.citacomsys.module.api.customer.CustomerRegistrationRequest
import com.citacomsys.module.api.customer.CustomerRepository
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.EntityType
import com.citacomsys.types.MeshHop
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import io.github.aakira.napier.Napier
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal class CustomerHubGateway(
    private val repository: CustomerRepository,
    private val json: Json,
    private val telemetryClient: TelemetryLogClient
) : HubServiceGateway {
    override val domain: String = "customers"
    override val entityType: EntityType? = EntityType.CUSTOMER

    override fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit) {
        route("customers") {
            // Get or Create Customer
            post {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val workspaceId = meta?.wsId ?: call.request.headers["X-Workspace-Id"] ?: ""
                val payload = call.receive<String>()
                val request =
                    json.decodeFromString(CustomerRegistrationRequest.serializer(), payload)

                ActiveRoutingChainHolder.set(routingChainJson)
                val profile = try {
                    repository.getOrCreateCustomer(request, request.workspaceId)
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }

                telemetryClient.trackApplicationEvent(
                    actorId = "mesh-node",
                    entityType = "CUSTOMER",
                    entityId = profile.id,
                    action = "CUSTOMER_REGISTER_HUB_RELAY",
                    summary = "Registered/Retrieved customerState ${profile.fullName} via Hub gateway",
                    workspaceId = workspaceId
                )

                call.respondText(
                    json.encodeToString(CustomerItem.serializer(), profile),
                    ContentType.Application.Json
                )
            }

            // Find By Id
            get("{id}") {
                val workspaceId = call.request.headers["X-Workspace-Id"] ?: ""
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val profile = repository.findById(id, workspaceId).getOrNull()
                if (profile != null) {
                    call.respondText(
                        json.encodeToString(Customer.serializer(), profile),
                        ContentType.Application.Json
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Search
            get("search") {
                val workspaceId = call.request.headers["X-Workspace-Id"] ?: ""
                val query = call.request.queryParameters["q"] ?: ""
                val paged = repository.fetchPaged(
                    "",
                    PageQuery(page = 1, size = 20, search = query, workspaceId = workspaceId)
                )
                val serializer = ListSerializer(CustomerItem.serializer())
                call.respondText(
                    json.encodeToString(serializer, paged.items),
                    ContentType.Application.Json
                )
            }

            // Delta Change List for Mesh Syncing
            get("change-list") {
                val workspaceId =
                    call.request.headers["X-Workspace-Id"] ?: call.request.queryParameters["wsId"]
                    ?: ""
                val branchId = call.request.queryParameters["branchId"].orEmpty()
                val since =
                    call.request.queryParameters["since"]?.let { runCatching { Instant.parse(it) }.getOrNull() }

                Napier.d(tag = "HUB_GATEWAY") { "Processing change list for workspace: $workspaceId, branch: $branchId, since: $since" }
                val changeList = repository.getChangeList(workspaceId, branchId, since)

                call.respondText(
                    json.encodeToString(
                        ChangeListResponse.serializer(
                            CustomerItem.serializer(),
                            String.serializer()
                        ), changeList
                    ),
                    ContentType.Application.Json
                )
            }

            delete("{id}") {
                val workspaceId =
                    call.request.headers["X-Workspace-Id"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest
                    )

                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val meta = call.hubSyncMetadata

                repository.delete(
                    id,
                    workspaceId,
                    actorId = meta?.actorId ?: "mesh-node",
                    actorName = meta?.actorName
                )

                telemetryClient.trackApplicationEvent(
                    actorId = meta?.actorId ?: "mesh-node",
                    entityType = "CUSTOMER",
                    entityId = id,
                    action = "CUSTOMER_DELETE_HUB_RELAY",
                    summary = "Soft-deleted customerState $id via Hub gateway",
                    workspaceId = workspaceId
                )
                call.respond(HttpStatusCode.OK)
            }

            // Paged Query
            post("query") {
                val payload = call.receive<String>()
                val query = json.decodeFromString(PageQuery.serializer(), payload)
                val results = repository.fetchPaged("", query)
                val serializer = PageResult.serializer(CustomerItem.serializer())
                call.respondText(
                    json.encodeToString(serializer, results),
                    ContentType.Application.Json
                )
            }

            // Record Ledger Transaction
            post("{customerId}/ledger") {
                val meta = call.hubSyncMetadata
                val routingChainJson = meta?.let {
                    json.encodeToString(ListSerializer(MeshHop.serializer()), it.routingChain)
                }

                val customerId = call.parameters["customerId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest
                )
                val workspaceId = meta?.wsId ?: call.request.headers["X-Workspace-Id"] ?: ""
                val payload = call.receive<String>()
                val request = json.decodeFromString(CustomerLedgerRequest.serializer(), payload)

                ActiveRoutingChainHolder.set(routingChainJson)
                val entry = try {
                    repository.recordTransaction(
                        customerId,
                        request.copy(workspaceId = workspaceId)
                    )
                } finally {
                    ActiveRoutingChainHolder.set(null)
                }

                telemetryClient.trackApplicationEvent(
                    actorId = "mesh-node",
                    entityType = "CUSTOMER",
                    entityId = customerId,
                    action = "CUSTOMER_LEDGER_HUB_RELAY",
                    summary = "Recorded ledger transaction for customerState $customerId via Hub gateway",
                    workspaceId = workspaceId
                )

                call.respondText(
                    json.encodeToString(CustomerLedgerEntry.serializer(), entry),
                    ContentType.Application.Json
                )
            }

            // Fetch Ledger Entries for a Sale
            get("{saleId}/ledger-sale") {
                val workspaceId = call.request.headers["X-Workspace-Id"] ?: ""
                val saleId =
                    call.parameters["saleId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val entries = repository.getLedgerForSale(saleId, workspaceId)
                val serializer = ListSerializer(CustomerLedgerEntry.serializer())
                call.respondText(
                    json.encodeToString(serializer, entries),
                    ContentType.Application.Json
                )
            }
        }
    }
}
