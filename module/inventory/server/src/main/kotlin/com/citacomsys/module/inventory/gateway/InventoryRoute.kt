package com.citacomsys.module.inventory.gateway

import com.citacomsys.core.persistence.plugin.getChangeListParameter
import com.citacomsys.core.persistence.plugin.readTransaction
import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.module.inventory.model.StockTransferPayload
import com.citacomsys.module.inventory.request.AdjustStockRequest
import com.citacomsys.module.inventory.request.PriceChangeRequest
import com.citacomsys.module.inventory.request.RestockRequest
import com.citacomsys.module.inventory.request.UpdateBatchMetadataRequest
import com.citacomsys.module.inventory.service.StockBatchService
import com.citacomsys.types.ProductModule
import com.citacomsys.types.UserCapability
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.inventory() {
    val service: StockBatchService by inject()

    multiTargetDeltaRoute()

    route("api/v1/stock-transfers") {
        authenticate("access-auth") {
            install(Authorize) {
                module = ProductModule.INVENTORY
                scope = SecurityScope.WORKSPACE
                requireCapabilities = setOf(UserCapability.MANAGE_INVENTORY)
                allowMeshReplay = false
            }

            post {
                val payload = call.receive<StockTransferPayload>()
                val workspaceId = call.resolvedWorkspaceId

                call.transactionManager.runInTransaction {
                    service.executeTransfer(payload, workspaceId).getOrThrow()
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("api/v1/stock-batches") {
        authenticate("access-auth", "device-auth") {
            install(Authorize) {
                module = ProductModule.INVENTORY
                scope = SecurityScope.WORKSPACE
                requireCapabilities = setOf(UserCapability.MANAGE_INVENTORY)
                allowMeshReplay = true
            }

            get {
                val since = call.getChangeListParameter()
                val workspaceId = call.resolvedWorkspaceId
                val branchId = call.request.queryParameters["branchId"]
                val orgId = call.resolveOrganizationId
                val item = call.readTransaction {
                    service.getChangeList(since, orgId, workspaceId, branchId)
                }
                call.respond(item)
            }

            get("/change-list") {
                val since = call.getChangeListParameter()
                val workspaceId = call.resolvedWorkspaceId
                val branchId = call.request.queryParameters["branchId"]
                val orgId = call.resolveOrganizationId
                val item = call.readTransaction {
                    service.getChangeList(since, orgId, workspaceId, branchId)
                }
                call.respond(item)
            }

            post {
                val request = call.receive<RestockRequest>()
                val workspaceId = call.resolvedWorkspaceId

                val item = call.writeTransaction {
                    service.restock(request, workspaceId).getOrThrow()
                }
                call.respond(item)
            }

            post("/restock") {
                val request = call.receive<RestockRequest>()
                val workspaceId = call.resolvedWorkspaceId

                val item = call.writeTransaction {
                    service.restock(request, workspaceId).getOrThrow()
                }
                call.respond(item)
            }

            post("/transfer") {
                val payload = call.receive<StockTransferPayload>()
                val workspaceId = call.resolvedWorkspaceId

                call.transactionManager.runInTransaction {
                    service.executeTransfer(payload, workspaceId).getOrThrow()
                }

                call.respond(HttpStatusCode.OK)
            }

            route("/{id}") {
                get {
                    val id = call.parameters["id"]!!
                    val workspaceId = call.resolvedWorkspaceId
                    val batch = call.readTransaction {
                        service.getById(id, workspaceId)
                    }
                    if (batch != null) {
                        call.respond(batch)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                put {
                    val request = call.receive<AdjustStockRequest>()
                    val item = call.transactionManager.runInTransaction {
                        service.adjust(request).getOrThrow()
                    }
                    call.respond(item)
                }

                put("/adjust") {
                    val request = call.receive<AdjustStockRequest>()
                    val item = call.transactionManager.runInTransaction {
                        service.adjust(request).getOrThrow()
                    }
                    call.respond(item)
                }

                put("/price") {
                    val request = call.receive<PriceChangeRequest>()
                    val workspaceId = call.resolvedWorkspaceId

                    call.writeTransaction {
                        service.updatePrice(request, workspaceId)
                    }

                    call.respond(Unit)
                }

                put("/meta") {
                    val request = call.receive<UpdateBatchMetadataRequest>()
                    val workspaceId = call.resolvedWorkspaceId

                    call.transactionManager.runInTransaction {
                        service.updateBatchMetadata(request, workspaceId = workspaceId)
                    }
                    call.respond(Unit)
                }

                put("/metadata") {
                    val request = call.receive<UpdateBatchMetadataRequest>()
                    val workspaceId = call.resolvedWorkspaceId

                    call.transactionManager.runInTransaction {
                        service.updateBatchMetadata(request, workspaceId = workspaceId)
                    }
                    call.respond(Unit)
                }

                delete {
                    val workspaceId = call.resolvedWorkspaceId
                    call.transactionManager.runInTransaction {
                        service.delete(
                            call.parameters["id"]!!,
                            workspaceId = workspaceId
                        )
                    }
                    call.respond(Unit)
                }
            }

            route("/{productId}/{batchId}") {
                get("/movement") {
                    val workspaceId = call.resolvedWorkspaceId
                    val productId = call.parameters["productId"]!!
                    val batchId = call.parameters["batchId"]!!
                    val since = call.getChangeListParameter()

                    val movements = call.readTransaction {
                        service.getStockMovementsForBatch(
                            productId = productId,
                            batchId = batchId,
                            since = since,
                            workspaceId = workspaceId,
                        )
                    }
                    call.respond(movements)
                }
            }
        }
    }
}
