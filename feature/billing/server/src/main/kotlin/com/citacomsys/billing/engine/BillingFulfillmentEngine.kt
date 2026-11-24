package com.citacomsys.billing.engine

import com.citacomsys.api.billing.*
import com.citacomsys.api.workspace.repository.WorkspaceResourceUsageRepository
import com.citacomsys.api.workspace.service.WorkspaceSubscriptionService
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.notification.service.SseService
import com.citacomsys.shared.event.SseMessage
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.shared.event.SubscriptionEvent
import kotlinx.serialization.json.Json
import mu.KotlinLogging

internal class BillingFulfillmentEngine(
    private val workspaceResourceRepository: WorkspaceResourceUsageRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val sseService: SseService,
    private val auditor: TelemetryAuditor,
    private val subscriptionService: WorkspaceSubscriptionService
) {

    private val logger = KotlinLogging.logger { }

    suspend fun apply(invoice: BillingRecord): Unit = dbQuery {

        when (val payload = invoice.quote.payload) {
            is SubscriptionBillingPayload -> {
                require(invoice.subscriptionVersion != null) {
                    "Subscription target mismatch version reference required"
                }

                subscriptionService.applySubscriptionChange(
                    workspaceId = invoice.workspaceId,
                    provisioned = payload.provisionedPackage,
                    type = payload.type,
                    expectedVersion = invoice.subscriptionVersion!!
                )
            }

            is SmsTopupBillingPayload -> {
                applySmsTopup(invoice.workspaceId, payload)
            }
        }

        publishRuntimeEvents(invoice)

        auditor.logApplicationEvent(
            action = "BILLING_FULFILLMENT_APPLIED",
            entityType = "PACKAGE",
            entityId = invoice.id,
            summary = "Applied workspace billing fulfillment for ${invoice.type.name} to workspace ${invoice.wsName}",
            level = TelemetryLevel.INFO,
            workspaceIdOverride = invoice.workspaceId
        )
    }

    private suspend fun applySmsTopup(
        workspaceId: String,
        payload: SmsTopupBillingPayload
    ) {
        payload.effects.forEach { effect ->
            when (effect) {
                is BillingEffect.AddSms -> {
                    workspaceResourceRepository.updateSmsBalance(workspaceId, effect.units)
                    logger.info { "Applied core network SMS top-up allocation of ${effect.units} units to workspace=$workspaceId" }
                }

                is BillingEffect.EnableModule -> {
                    logger.info { "Promotional feature authorization trigger activated via top-up path for workspace=$workspaceId" }
                }
            }
        }

        auditor.logApplicationEvent(
            action = "SMS_TOPUP_FULFILLED",
            entityType = "PACKAGE",
            entityId = workspaceId,
            summary = "Applied standalone SMS bundle credits top-up of ${payload.totalSms} units to workspace $workspaceId",
            level = TelemetryLevel.INFO,
            workspaceIdOverride = workspaceId
        )
    }

    private suspend fun publishRuntimeEvents(
        invoice: BillingRecord
    ) {
        val workspace = workspaceRepository.get(invoice.workspaceId).getOrThrow()
        val subscriptionMessage = SseMessage(
            event = SubscriptionEvent.ID,
            data = Json.encodeToString(
                SubscriptionEvent(wsId = workspace.id, organizationId = workspace.organizationId)
            ),
            topic = "SUBSCRIPTION",
            organizationId = workspace.organizationId,
            workspaceId = workspace.id
        )

        sseService.broadcast(subscriptionMessage)
    }
}
