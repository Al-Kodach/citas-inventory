package com.citacomsys.sales.gateway

import com.citacomsys.api.sales.dto.CreateSaleRequest
import com.citacomsys.api.sales.dto.PreviewSaleRequest
import com.citacomsys.api.sales.service.DigitalReceiptService
import com.citacomsys.core.persistence.plugin.getChangeListParameter
import com.citacomsys.core.persistence.plugin.readTransaction
import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.plugin.userPrincipal
import com.citacomsys.sales.service.InventoryQueryService
import com.citacomsys.sales.service.OnlineSaleService
import com.citacomsys.sales.usecase.IngestOfflineSaleUseCase
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.ProductModule
import com.citacomsys.types.UserCapability
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.onlineSalesRouting(
    onlineService: OnlineSaleService,
    digitalReceiptService: DigitalReceiptService
) {
    authenticate("access-auth") {
        route("api/v1/sales") {
            install(Authorize) {
                module = ProductModule.SALES
                scope = SecurityScope.WORKSPACE
                requireCapabilities =
                    setOf(
                        UserCapability.PROCESS_SALES,
                        UserCapability.MANAGE_INVENTORY
                    )
            }

            get {
                val branchId = call.request.queryParameters["branchId"]
                    ?: call.request.header("X-View-Branch-Id")
                    ?: call.request.header("X-Branch-Id")


                val workspaceId = call.resolvedWorkspaceId
                val startDate = call.request.queryParameters["startedAt"]
                val endDate = call.request.queryParameters["endAt"]

                val sales = call.transactionManager.runReadOnly {
                    onlineService.getSales(
                        workspaceId = workspaceId,
                        branchId = branchId,
                        startDate = startDate?.let { kotlin.time.Instant.parse(it) },
                        endDate = endDate?.let { kotlin.time.Instant.parse(it) }
                    )
                }
                call.respond(sales)
            }

            post {
                val saleResult = call.writeTransaction {
                    val workspaceId = call.resolvedWorkspaceId
                    val principal = call.userPrincipal()

                    val request = call.receive<CreateSaleRequest>().copy(
                        actorId = principal.userId
                    )

                    val orgId = call.resolveOrganizationId

                    onlineService.createSale(
                        request = request,
                        workspaceId = workspaceId,
                        orgId = orgId
                    ).getOrThrow()
                }

                call.respond(saleResult)
            }

            post("/previewInvoice") {
                val workspaceId = call.resolvedWorkspaceId

                val preview = call.transactionManager.runReadOnly {
                    val request = call.receive<PreviewSaleRequest>()
                    onlineService.previewSale(request, workspaceId).getOrThrow()
                }
                call.respond(preview)
            }

            route("/{saleId}") {
                get("/record") {
                    val workspaceId = call.resolvedWorkspaceId
                    val record = call.readTransaction {
                        onlineService.getSaleRecord(
                            workspaceId = workspaceId,
                            saleId = call.parameters["saleId"]!!
                        )
                    }
                    call.respond(record)
                }

                get {
                    val workspaceId = call.resolvedWorkspaceId
                    val saleDetails = call.readTransaction {
                        onlineService.getSaleDetails(
                            workspaceId = workspaceId,
                            saleId = call.parameters["saleId"]!!
                        )
                    }
                    call.respond(saleDetails)
                }

                post("/receipt/sms") {
                    val phone = call.receive<PhoneNumber>()
                    val workspaceId = call.resolvedWorkspaceId

                    val result = call.writeTransaction {
                        digitalReceiptService.sendSmsReceipt(
                            saleId = call.parameters["saleId"]!!,
                            phone = phone,
                            workspaceId = workspaceId
                        ).getOrThrow()
                    }
                    call.respond(result)
                }
            }

            post("/receipt-txt/sms") {
                val request = call.receive<Map<String, String>>()

                val phone = request["phone"] ?: error("Missing phone number")
                val receipt = request["receipt"] ?: error("Missing receipt")

                val workspaceId = call.resolvedWorkspaceId

                val result = call.transactionManager.runInTransaction {
                    digitalReceiptService.sendReceipt(
                        phone = phone,
                        receipt = receipt,
                        workspaceId = workspaceId
                    ).getOrThrow()
                }

                call.respond(result)
            }
        }
    }
}

internal fun Route.offlineSalesRoute(
    offlineSaleService: IngestOfflineSaleUseCase
) {
    route("api/v1/sales/sync") {
        authenticate("device-access-auth") {
            install(Authorize) {
                module = ProductModule.SALES
                scope = SecurityScope.WORKSPACE
            }

            post {
                val workspaceId = call.resolvedWorkspaceId
                val request = call.receive<CreateSaleRequest>()

                val result = call.writeTransaction {
                    offlineSaleService.execute(
                        sale = request,
                        workspaceId = workspaceId
                    )
                }
                call.respond(result)
            }
        }
    }
}

internal fun Route.sellableProductsRouting(
    queryService: InventoryQueryService
) {
    authenticate("access-auth", "device-access-auth") {
        route("api/v1/sellable") {
            install(Authorize) {
                module = ProductModule.SALES
                scope = SecurityScope.WORKSPACE
                requireCapabilities =
                    setOf(
                        UserCapability.PROCESS_SALES,
                        UserCapability.MANAGE_INVENTORY
                    )
            }

            get {
                val workspaceId = call.resolvedWorkspaceId
                val organizationId = call.resolveOrganizationId
                val branchId = call.request.queryParameters["branchId"] ?: workspaceId

                val products = call.readTransaction {
                    queryService.getSnapshotChanges(
                        call.getChangeListParameter(),
                        organizationId,
                        workspaceId,
                        branchId
                    )
                }

                call.respond(products)
            }
        }
    }
}
