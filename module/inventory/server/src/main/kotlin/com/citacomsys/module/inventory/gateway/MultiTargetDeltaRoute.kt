package com.citacomsys.module.inventory.gateway

import com.citacomsys.core.persistence.plugin.readTransaction
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.module.inventory.service.BaseProductService
import com.citacomsys.module.inventory.service.StockBatchService
import com.citacomsys.types.BatchItem
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.MultiTargetDeltaRequest
import com.citacomsys.types.MultiTargetDeltaResponse
import com.citacomsys.types.ProductModule
import com.citacomsys.types.UserCapability
import com.citacomsys.types.WorkspaceDeltaResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.time.Instant

fun Route.multiTargetDeltaRoute() {
    val productService: BaseProductService by inject()
    val stockBatchService: StockBatchService by inject()

    route("api/v1/products/deltas") {
        authenticate("access-auth", "device-auth") {
            install(Authorize) {
                module = ProductModule.INVENTORY
                scope = SecurityScope.ORGANIZATION
                requireCapabilities = setOf(UserCapability.MANAGE_INVENTORY)
                allowMeshReplay = true
            }

            post {
                val request = call.receive<MultiTargetDeltaRequest>()
                val organizationId = call.resolveOrganizationId

                val workspaceDeltas = mutableMapOf<String, WorkspaceDeltaResponse>()

                call.readTransaction {
                    for ((workspaceId, branchCursors) in request.workspaces) {
                        if (branchCursors.isEmpty()) continue

                        val minProductSync = branchCursors.mapNotNull { it.lastProductSyncAt }
                            .minOrNull() ?: Instant.parse("2026-01-01T00:00:00Z")

                        val firstBranchId = branchCursors.first().branchId
                        val productChanges = productService.getProductItemChanges(
                            since = minProductSync,
                            organizationId = organizationId,
                            workspaceId = workspaceId,
                            branchId = firstBranchId
                        )

                        val branchBatches = mutableMapOf<String, ChangeListResponse<BatchItem, String>>()
                        for (cursor in branchCursors) {
                            val batchSyncSince = cursor.lastBatchSyncAt ?: Instant.fromEpochMilliseconds(0)
                            val batchChanges = stockBatchService.getChangeList(
                                since = batchSyncSince,
                                organizationId = organizationId,
                                workspaceId = workspaceId,
                                branchId = cursor.branchId
                            )
                            branchBatches[cursor.branchId] = batchChanges
                        }

                        workspaceDeltas[workspaceId] = WorkspaceDeltaResponse(
                            products = productChanges,
                            branchBatches = branchBatches
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, MultiTargetDeltaResponse(workspaceDeltas))
            }
        }
    }
}
