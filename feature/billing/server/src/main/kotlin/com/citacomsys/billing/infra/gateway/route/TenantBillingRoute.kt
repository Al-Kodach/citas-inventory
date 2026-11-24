package com.citacomsys.billing.infra.gateway.route

import com.citacomsys.api.billing.PaymentSubmission
import com.citacomsys.api.billing.service.BillingService
import com.citacomsys.api.platform.uiModel.PreviewRequest
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.persistence.plugin.readTransaction
import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.core.security.withActionConfirmation
import com.citacomsys.types.ActionConfirmationPurpose
import com.citacomsys.types.UserCapability
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.tenantBilling() {
    val service: BillingService by inject()
    val telemetryAuditor: TelemetryAuditor by inject()
    val alertService: SecurityAlertService by inject()


    route("api/v1/billing") {
        install(Authorize) {
            requireCapabilities = setOf(UserCapability.MANAGE_BILLING)
        }

        // SENSITIVE OPERATIONS: Require Action Token (ACT) issued after biometric verification
        authenticate("action-auth") {

            post("/cancel/{invoiceId}") {
                withActionConfirmation(
                    telemetryAuditor,
                    alertService,
                    ActionConfirmationPurpose.MANAGE_BILLING
                ) { principal ->
                    val invoiceId = call.parameters["invoiceId"]
                        ?: return@withActionConfirmation call.respond(HttpStatusCode.BadRequest)

                    val reason = call.receiveText()

                    service.cancelInvoice(
                        invoiceId = invoiceId,
                        workspaceId = principal.workspaceId,
                        sessionId = principal.sessionId,
                        reason = reason
                    ).getOrThrow()

                    call.respond(HttpStatusCode.OK, mapOf("message" to "Invoice cancelled"))
                }
            }

            /**
             * POST /api/v1/billing/renewal
             * Initiates a new billing request (Upgrade, SMS Top-up, or Subscription).
             */
            post("renewal/{billingTermId}") {
                withActionConfirmation(
                    telemetryAuditor,
                    alertService,
                    ActionConfirmationPurpose.MANAGE_BILLING
                ) { principal ->

                    val result = service.createRenewalInvoice(
                        workspaceId = principal.workspaceId,
                        userId = principal.userId,
                        sessionId = principal.sessionId
                    )

                    call.respond(HttpStatusCode.Created, result)
                }
            }

            /**
             * POST /api/v1/billing/{invoiceId}/submit-payment
             * Called when the owner clicks "Payment Made" and provides MoMo Transaction ID.
             */
            post("/{invoiceId}/submit-payment") {
                withActionConfirmation(
                    telemetryAuditor,
                    alertService,
                    ActionConfirmationPurpose.MANAGE_BILLING
                ) { principal ->
                    val submission = call.receive<PaymentSubmission>()

                    service.paymentSubmitted(
                        info = submission,
                        workspaceId = principal.workspaceId,
                        userId = principal.userId,
                        sessionId = principal.sessionId
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Payment details submitted for verification")
                    )
                }
            }

            post("sms-topup/{pkgId}") {
                withActionConfirmation(
                    telemetryAuditor,
                    alertService,
                    ActionConfirmationPurpose.MANAGE_BILLING
                ) { principal ->
                    val pkgId = call.parameters["pkgId"]
                        ?: return@withActionConfirmation call.respond(HttpStatusCode.BadRequest)


                    val record = call.writeTransaction {
                        service.createSmsTopupInvoice(
                            workspaceId = principal.workspaceId,
                            packageId = pkgId,
                            userId = principal.userId,
                            sessionId = principal.sessionId
                        )
                    }
                    call.respond(HttpStatusCode.Created, record)
                }
            }
        }

        // READ-ONLY OPERATIONS: Standard access auth
        authenticate("access-auth") {
            /**
             * GET /api/v1/billing/active
             * Returns the current pending or submitted invoice for the pharmacy profile.
             */
            get("active") {
                val workspaceId = call.resolvedWorkspaceId

                val invoice = call.readTransaction {
                    service.getActiveInvoices(workspaceId)
                }

                call.respond(invoice)
            }

            get("{invoiceId}") {
                val invoiceId = call.parameters["invoiceId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val workspaceId = call.resolvedWorkspaceId
                val record = call.readTransaction {
                    service.getInvoiceById(invoiceId, workspaceId)
                }
                call.respond(record)
            }

            /**
             * GET /api/v1/billing/history
             * Returns the payment history (transactions) for the pharmacy.
             */
            get("history") {
                val workspaceId = call.resolvedWorkspaceId

                val history = call.readTransaction { service.getPaymentHistory(workspaceId) }

                call.respond(history)
            }

            get("previewInvoice") {
                val type = call.receive<PreviewRequest>()
                val preview = service.invoicePreviewRequest(type)
                call.respond(preview)
            }
        }
    }
}
