package com.citacomsys.billing.engine

import com.citacomsys.core.notification.backgroundJobHandler.BackgroundJobHandler
import com.citacomsys.api.billing.BillingAction
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.api.billing.BillingRepository
import com.citacomsys.api.billing.SmsTopupBillingPayload
import com.citacomsys.api.platform.model.ProvisionedPackage
import com.citacomsys.api.billing.service.BillingService
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.types.BillingStatus
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
@SerialName("BillingJobState")
sealed class BillingJobState {
    @Serializable
    @SerialName("PENDING")
    data class PendingFulfillment(
        val invoiceId: String
    ) : BillingJobState()

    @Serializable
    @SerialName("TRIAL_CONVERSION")
    data class TrialConversion(
        val workspaceId: String,
        val provisioned: ProvisionedPackage,
        val generatedAt: Instant
    ) : BillingJobState()
}

internal class BillingFulfillmentScheduleJobHandler(
    private val billingRepository: BillingRepository,
    private val fulfillmentEngine: BillingFulfillmentEngine,
    private val workspaceRepository: WorkspaceRepository,
    private val telemetryAuditor: TelemetryAuditor,
    private val notificationService: NotificationService,
    private val billingService: BillingService,
    private val userRepository: UserPort,
    private val json: Json
) : BackgroundJobHandler {
    private val logger = KotlinLogging.logger { }

    override val id: String = billingService.serviceId

    override suspend fun execute(payload: String): Unit = dbQuery {
        when (val state = json.decodeFromString<BillingJobState>(payload)) {
            is BillingJobState.PendingFulfillment -> handleFulfillment(state)
            is BillingJobState.TrialConversion -> {
                billingService.ensureOnboardingInvoice(workspaceId = state.workspaceId)
            }
        }
    }

    private suspend fun handleFulfillment(
        state: BillingJobState.PendingFulfillment
    ) {

        val invoice = billingRepository.find(state.invoiceId)
            ?: throw NotFoundException("Invoice not found")

        when (invoice.status) {
            BillingStatus.FULFILLED,
            BillingStatus.CANCELED,
            BillingStatus.EXPIRED -> {
                return
            }

            BillingStatus.PAID -> {
                // continue
            }

            else -> {
                return
            }
        }

        try {
            fulfillmentEngine.apply(invoice)

            billingRepository.updateFulfilmentDetails(
                invoiceId = invoice.id,
                attempts = invoice.fulfillmentAttempts + 1,
                error = null,
                fulfilledAt = Clock.System.now()
            )

            billingRepository.save(
                invoice.copy(
                    status = BillingStatus.FULFILLED,
                    fulfillmentAttempts = invoice.fulfillmentAttempts + 1,
                    lastFulfillmentError = null,
                    fulfilledAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
            )

            if (invoice.quote.payload !is SmsTopupBillingPayload) {
                billingRepository.expireSubscriptionInvoices(
                    workspaceId = invoice.workspaceId,
                    maxVersion = invoice.subscriptionVersion!! + 1
                )
            }

            telemetryAuditor.logApplicationEvent(
                action = BillingAction.BILLING_FULFILLED.name,
                summary = "Invoice ${invoice.referenceCode} fulfilled successfully.",
                level = TelemetryLevel.INFO,
                entityType = "PACKAGE",
                entityId = invoice.id,
                workspaceIdOverride = invoice.workspaceId
            )

            sendSuccessNotification(invoice)
        } catch (e: Exception) {
            logger.error(e) { "Failed to apply subscription" }

            billingRepository.save(
                invoice.copy(
                    status = BillingStatus.FULFILMENT_FAILED,
                    fulfillmentAttempts = invoice.fulfillmentAttempts + 1,
                    lastFulfillmentError = e.message ?: "Failed to apply subscription",
                    fulfilledAt = null,

                    updatedAt = Clock.System.now()
                )
            )
            throw e 
        }
    }

    private suspend fun sendSuccessNotification(invoice: BillingRecord): Unit = dbQuery {
        val workspace = workspaceRepository.get(invoice.workspaceId).getOrThrow()
        val owner = userRepository.findOwner(workspace.organizationId) ?: return@dbQuery

        val message = """
            Payment confirmed.
            Ref: ${invoice.referenceCode}
            Your account resources have been updated successfully.
            Thank you for choosing CITACOM SYSTEMS.
        """.trimIndent()

        notificationService.sendManagement(
            organizationId = workspace.organizationId,
            workspaceId = invoice.workspaceId,
            phone = owner.phone,
            email = owner.email,
            userId = owner.id,
            message = message
        )
    }
}
