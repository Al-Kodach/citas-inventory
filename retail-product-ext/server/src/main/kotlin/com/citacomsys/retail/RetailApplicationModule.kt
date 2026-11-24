package com.citacomsys.retail

import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.module.inventory.gateway.configureGenericProductRoute
import com.citacomsys.retail.api.RetailProductRepository
import com.citacomsys.retail.api.dto.RetailMetadataUpdateRequest
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.retail.repository.ExposedRetailProductRepository
import com.citacomsys.retail.service.RetailProductService
import com.citacomsys.types.UserCapability
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.koin.ktor.ext.inject

fun Application.configureRetailService() {
    val service: RetailProductService by inject(RetailProductService::class.java)

    loadKoinModules(
        listOf(
            module {
                single<RetailProductRepository> {
                    ExposedRetailProductRepository(get())
                }
                single {
                    RetailProductService(get(), get(), get())
                }
            }
        )
    )

    routing {
        route("api/v1/inventory/products/retail") {
            install(Authorize) {
                scope = SecurityScope.WORKSPACE
                requireCapabilities = setOf(UserCapability.MANAGE_INVENTORY)
            }
            authenticate("access-auth", "device-auth") {
                configureGenericProductRoute<RetailProduct>(
                    path = "",
                    service = this@routing.inject<RetailProductService>().value,
                    throttler = this@routing.inject<SecurityThrottlerService>().value,
                    telemetryAuditor = this@routing.inject<TelemetryAuditor>().value,
                    alertService = this@routing.inject<SecurityAlertService>().value,
                )

                route("/{id}") {
                    patch {
                        val id = call.parameters["id"]
                            ?: return@patch call.respond(HttpStatusCode.BadRequest)

                        val request = call.receive<RetailMetadataUpdateRequest>()
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

                        val request = call.receive<RetailMetadataUpdateRequest>()
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
                }
            }
        }

        // Retail Specific sub-routes can be added here
        retailInventory()
    }
}
