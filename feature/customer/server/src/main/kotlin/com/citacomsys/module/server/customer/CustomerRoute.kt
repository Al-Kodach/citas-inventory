package com.citacomsys.module.server.customer

import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.module.api.customer.ConsentUpdateRequest
import com.citacomsys.module.api.customer.CustomerRegistrationRequest
import com.citacomsys.module.api.customer.CustomerLedgerRequest
import com.citacomsys.module.api.customer.CustomerService
import com.citacomsys.types.PageQuery
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.persistence.plugin.getChangeListParameter
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityContextKey
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.types.EntityType
import com.citacomsys.types.UserCapability
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.customerRoutes() {
    val customerService: CustomerService by inject()
    val telemetryAuditor: TelemetryAuditor by inject()

    route("api/v1/customers") {
        authenticate("access-auth", "device-access-auth") {
            install(Authorize) {
                requireCapabilities = setOf(UserCapability.VIEW_CUSTOMERS)
            }

            /**
             * POST: Get or Create Customer profile on-the-fly
             */
            post {
                val request = call.receive<CustomerRegistrationRequest>()
                val organizationId = call.resolveOrganizationId

                if (request.fullName.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Name variable cannot be blank."
                    )
                }

                val ctx = call.attributes[SecurityContextKey]


                val profile = call.writeTransaction {
                    customerService.getOrCreateCustomer(request, organizationId)
                }

                telemetryAuditor.logApplicationEvent(
                    action = "CUSTOMER_CREATED",
                    entityType = EntityType.CUSTOMER.name,
                    entityId = profile.id,
                    summary = "Customer ${profile.fullName} created by actor ${ctx.actorName}",
                    level = TelemetryLevel.INFO,
                    organizationIdOverride = organizationId,
                    workspaceIdOverride = profile.workspaceId
                )

                call.respond(HttpStatusCode.OK, profile)
            }

            get("/change-list") {
                val workspaceId = call.requireQueryParameter("wsId")
                val branchId = call.queryParameters["branchId"]
                val since = call.getChangeListParameter()

                val result = customerService.getChangeList(
                    workspaceId = workspaceId,
                    branchId = branchId.orEmpty(),
                    since = since
                )

                call.respond(HttpStatusCode.OK, result)
            }


            /**
             * POST: Paged query for customers
             */
            post("/query") {
                val organizationId = call.resolveOrganizationId
                val query = call.receive<PageQuery>()

                val results = customerService.fetchPaged(
                    organizationId, query
                )
                call.respond(HttpStatusCode.OK, results)
            }

            route("/{id}") {

                /**
                 * GET: Lookup precise profile details by explicit record key pointer
                 */
                get {
                    val workspaceId = call.requireQueryParameter("wsId")

                    val id = call.parameters["id"]
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing Customer ID parameter."
                        )


                    val profile = customerService.findById(
                        customerId = id,
                        workspaceId = workspaceId
                    )
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            "Customer not found."
                        )

                    call.respond(HttpStatusCode.OK, profile)
                }

                delete {
                    val workspaceId = call.requireQueryParameter("wsId")
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing Customer ID parameter."
                        )
                    call.writeTransaction {
                        customerService.delete(workspaceId, id)
                    }
                    call.respond(HttpStatusCode.OK)
                }

                /**
                 * PUT: Expressly toggle alert subscription statuses (GDPR Compliance Safeguard)
                 */
                put("/consent") {
                    val request = call.receive<Map<String, Any>>()
                    val isAlertsConsented =
                        (request["request"] as ConsentUpdateRequest).isAlertsConsented
                    val workspaceId = request["wsId"] as String

                    val id = call.parameters["id"]
                        ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing Customer ID parameter."
                        )

                    val organizationId = call.resolveOrganizationId

                    val isUpdated = call.writeTransaction {
                        customerService.updateConsent(
                            workspaceId = workspaceId,
                            customerId = id,
                            isConsented = isAlertsConsented
                        )
                    }

                    call.respond(HttpStatusCode.OK, isUpdated)
                }

                /**
                 * POST: Record a financial transaction for a customer (Payment, Credit Purchase, etc.)
                 */
                post("/ledger") {
                    val id = call.parameters["id"]
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing Customer ID parameter."
                        )

                    val request = call.receive<CustomerLedgerRequest>()
                    val ctx = call.attributes[SecurityContextKey]
                    val organizationId = call.resolveOrganizationId

                    val finalRequest = request.copy(
                        actorId = ctx.actorId,
                        actorName = ctx.actorName
                    )

                    val entry = call.writeTransaction {
                        customerService.recordTransaction(
                            id,
                            finalRequest
                        )
                    }

                    telemetryAuditor.logApplicationEvent(
                        action = "CUSTOMER_LEDGER_TRANSACTION",
                        entityType = EntityType.CUSTOMER.name,
                        entityId = id,
                        summary = "Customer transaction (${finalRequest.type} of ${finalRequest.amountMinor}) recorded by actor ${ctx.actorName}",
                        level = TelemetryLevel.INFO
                    )

                    call.respond(HttpStatusCode.OK, entry)
                }
            }
        }
    }
}
