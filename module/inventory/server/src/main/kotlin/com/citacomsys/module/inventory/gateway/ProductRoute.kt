package com.citacomsys.module.inventory.gateway

import com.citacomsys.core.persistence.plugin.getChangeListParameter
import com.citacomsys.core.persistence.plugin.readTransaction
import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.plugin.auditActorId
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.module.inventory.dto.ProductMetadataUpdateRequest
import com.citacomsys.module.inventory.service.BaseProductService
import com.citacomsys.module.inventory.service.ProductService
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.PageQuery
import com.citacomsys.types.ProductItem
import com.citacomsys.types.UserCapability
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureBaseProductService() {
    val service: BaseProductService by inject()

    routing {
        route("api/v1/inventory/products") {
            install(Authorize) {
                scope = SecurityScope.WORKSPACE
                requireCapabilities = setOf(UserCapability.MANAGE_INVENTORY)
            }
            authenticate("access-auth", "device-auth") {
                get("/sync") {
                    val since = call.getChangeListParameter()
                    val organizationId = call.resolveOrganizationId
                    val workspaceId = call.resolvedWorkspaceId
                    val branchId = call.request.queryParameters["branchId"] ?: workspaceId

                    val itemChanges = call.transactionManager.runReadOnly {
                        service.getProductItemChanges(
                            since = since,
                            organizationId = organizationId,
                            workspaceId = workspaceId,
                            branchId = branchId
                        )
                    }
                    call.respond(itemChanges)
                }

                post {
                    val request = call.receive<ProductItem>()
                    val wsId = call.resolvedWorkspaceId
                    val createdProduct = call.writeTransaction {
                        service.saveProduct(request, wsId)
                    }
                    call.respond(HttpStatusCode.Created, createdProduct)
                }

                post("/query") {
                    val query = call.receive<PageQuery>()
                    val workspaceId = call.resolvedWorkspaceId

                    val products = call.readTransaction {
                        service.fetch(workspaceId = workspaceId, query)
                    }
                    call.respond(products)
                }

                route("/{id}") {
                    get {
                        val id = call.parameters["id"]
                            ?: return@get call.respond(HttpStatusCode.BadRequest)

                        val workspaceId = call.resolvedWorkspaceId
                        val product = call.readTransaction {
                            service.getById(id, workspaceId = workspaceId)
                        } ?: return@get call.respond(HttpStatusCode.NotFound)

                        call.respond(product)
                    }

                    patch {
                        val id = call.parameters["id"]
                            ?: return@patch call.respond(HttpStatusCode.BadRequest)
                        val request = call.receive<ProductMetadataUpdateRequest>()
                        val workspaceId = call.resolvedWorkspaceId

                        val updatedProduct = call.writeTransaction {
                            service.updateMetadata(
                                id = id,
                                workspaceId = workspaceId,
                                request = request
                            )
                        }

                        call.respond(HttpStatusCode.OK, updatedProduct)
                    }

                    put {
                        val id = call.parameters["id"]
                            ?: return@put call.respond(HttpStatusCode.BadRequest)
                        val request = call.receive<ProductMetadataUpdateRequest>()
                        val workspaceId = call.resolvedWorkspaceId

                        val updatedProduct = call.writeTransaction {
                            service.updateMetadata(
                                id = id,
                                workspaceId = workspaceId,
                                request = request
                            )
                        }

                        call.respond(HttpStatusCode.OK, updatedProduct)
                    }

                    delete {
                        val id = call.parameters["id"]
                            ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val workspaceId = call.resolvedWorkspaceId
                        val actorId = call.auditActorId

                        call.transactionManager.runInTransaction {
                            service.deleteProduct(
                                productId = id,
                                workspaceId = workspaceId,
                                actorId = actorId
                            ).getOrThrow()
                        }

                        call.respond(HttpStatusCode.NoContent)
                    }

                    delete("/purge") {
                        val id = call.parameters["id"]
                            ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val workspaceId = call.resolvedWorkspaceId
                        val actorId = call.auditActorId

                        call.transactionManager.runInTransaction {
                            service.purgeProductAndAllAssociatedBatchesPermanently(
                                productId = id,
                                workspaceId = workspaceId,
                                actorId = actorId
                            )
                        }

                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}

/**
 * Universal, reusable route factory.
 * Eliminates duplicate endpoint boilerplate across multiple inventory modules.
 */
inline fun <reified T : BaseProduct> Route.configureGenericProductRoute(
    path: String,
    service: ProductService<T>,
    throttler: SecurityThrottlerService,
    telemetryAuditor: TelemetryAuditor,
    alertService: SecurityAlertService
) {
    route(path) {
        authenticate("access-auth", "device-auth") {
            get("/sync") {
                val since = call.getChangeListParameter()
                val organizationId = call.resolveOrganizationId
                val workspaceId = call.resolvedWorkspaceId
                val branchId = call.request.queryParameters["branchId"] ?: workspaceId

                val snapshotChanges = call.transactionManager.runReadOnly {
                    service.getSnapshotChanges(
                        since = since,
                        organizationId = organizationId,
                        workspaceId = workspaceId,
                        branchId = branchId
                    )
                }
                call.respond(snapshotChanges)
            }

            // CREATE NEW PRODUCT
            post {
                val request = call.receive<T>()
                val wsId = call.resolvedWorkspaceId
                val createdProduct = call.writeTransaction {
                    service.saveProduct(request, wsId)
                }
                call.respond(HttpStatusCode.Created, createdProduct)
            }

            // PAGINATED READ
            post("/query") {
                val query = call.receive<PageQuery>()
                val workspaceId = call.resolvedWorkspaceId

                val products = call.readTransaction {
                    service.fetch(workspaceId = workspaceId, query)
                }
                call.respond(products)
            }

            // SINGLE RECOVERY AND PURGES
            route("/{id}") {
                get {
                    val id =
                        call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                    val workspaceId = call.resolvedWorkspaceId
                    val product = call.readTransaction {
                        service.getById(id, workspaceId = workspaceId)
                    } ?: return@get call.respond(HttpStatusCode.NotFound)

                    call.respond(product)
                }

                delete {
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val workspaceId = call.resolvedWorkspaceId
                    val actorId = call.auditActorId

                    call.transactionManager.runInTransaction {
                        service.deleteProduct(
                            productId = id,
                            workspaceId = workspaceId,
                            actorId = actorId
                        ).getOrThrow()
                    }

                    call.respond(HttpStatusCode.NoContent)
                }

                delete("/purge") {
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val workspaceId = call.resolvedWorkspaceId
                    val actorId = call.auditActorId

                    call.transactionManager.runInTransaction {
                        service.purgeProductAndAllAssociatedBatchesPermanently(
                            productId = id,
                            workspaceId = workspaceId,
                            actorId = actorId
                        )
                    }

                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
