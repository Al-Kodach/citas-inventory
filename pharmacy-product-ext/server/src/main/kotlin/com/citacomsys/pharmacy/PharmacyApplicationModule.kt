package com.citacomsys.pharmacy

import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.module.inventory.gateway.configureGenericProductRoute
import com.citacomsys.pharmacy.api.dto.PharmacyMetadataUpdateRequest
import com.citacomsys.pharmacy.di.pharmacyAppModuleDi
import com.citacomsys.pharmacy.service.PharmacyProductService
import com.citacomsys.types.UserCapability
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules
import org.koin.ktor.ext.inject

fun Application.configurePharmacyService() {
    loadKoinModules(listOf(pharmacyAppModuleDi))

    routing {
        route("api/v1/inventory/products/pharmacy") {
            install(Authorize) {
                scope = SecurityScope.WORKSPACE
                requireCapabilities = setOf(UserCapability.MANAGE_INVENTORY)
            }

            configureGenericProductRoute(
                path = "",
                service = this@routing.inject<PharmacyProductService>().value,
                throttler = this@routing.inject<SecurityThrottlerService>().value,
                telemetryAuditor = this@routing.inject<TelemetryAuditor>().value,
                alertService = this@routing.inject<SecurityAlertService>().value,
            )

            route("/{id}") {
                patch {
                    val pharmacyService: PharmacyProductService by inject()

                    val id = call.parameters["id"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<PharmacyMetadataUpdateRequest>()
                    val workspaceId = call.resolvedWorkspaceId

                    val updatedProduct = call.writeTransaction {
                        pharmacyService.updateMetadata(
                            id = id,
                            workspaceId = workspaceId,
                            request = request
                        )
                    }

                    call.respond(HttpStatusCode.OK, updatedProduct)
                }

                put {
                    val pharmacyService: PharmacyProductService by inject()

                    val id = call.parameters["id"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<PharmacyMetadataUpdateRequest>()
                    val workspaceId = call.resolvedWorkspaceId

                    val updatedProduct = call.writeTransaction {
                        pharmacyService.updateMetadata(
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
}
