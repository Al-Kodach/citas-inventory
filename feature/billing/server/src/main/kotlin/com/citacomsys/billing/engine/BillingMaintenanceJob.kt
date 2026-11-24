package com.citacomsys.billing.engine

import com.citacomsys.api.billing.BillingRepository
import com.citacomsys.api.billing.service.BillingService
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.core.domain.result.SystemActors.SYSTEM_ID
import com.citacomsys.core.notification.service.SmsService
import com.citacomsys.core.persistence.table.tenant.WorkspaceSubscriptionsTable
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.types.SubscriptionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

interface BillingMaintenanceJob {
    fun start()
}

internal class BillingMaintenanceJobImpl(
    private val billingRepository: BillingRepository,
    private val billingService: BillingService,
    private val workspaceRepository: WorkspaceRepository,
    private val smsService: SmsService,
    private val scope: CoroutineScope,
    private val userRepo: UserPort
) : BillingMaintenanceJob {

    private val logger = KotlinLogging.logger { }

    override fun start() {
        scope.launch {
            while (isActive) {
                runCatching { runCycle() }.onFailure {
                    logger.error(it) {
                        "Billing maintenance failed"
                    }
                }

                delay(6.hours)
            }
        }
    }

    private suspend fun runCycle() = dbQuery {

        expireInvoices()

        generateRenewals()

        generateTrialReminders()
    }

    private suspend fun expireInvoices() {
        val expired = billingRepository.expireInvoices(Clock.System.now())

        if (expired > 0) {
            logger.info {
                "Expired $expired invoices"
            }
        }
    }

    private suspend fun generateRenewals() {

        val horizon = Clock.System.now() + 14.days

        billingRepository
            .findTenantsNeedingRenewal(horizon)
            .forEach { workspaceId ->
                runCatching {
                    billingService.ensureRenewalInvoice(workspaceId)
                }
            }
    }

    private data class WorkspaceInfo(
        val workspaceId: String,
        val expireDate: Instant?
    )

    private suspend fun generateTrialReminders() {
        val now = Clock.System.now()

        val trailSubscription = WorkspaceSubscriptionsTable.selectAll().where {
            WorkspaceSubscriptionsTable.status eq SubscriptionStatus.TRIAL
        }.map { row ->
            WorkspaceInfo(
                workspaceId = row[WorkspaceSubscriptionsTable.workspaceId].toString(),
                expireDate = row[WorkspaceSubscriptionsTable.endedAt]
            )
        }

        trailSubscription.forEach { info ->
            val trialExpiry = info.expireDate ?: return@forEach
            val invoice = billingRepository.findTrialConversionInvoice(info.workspaceId)

            if (invoice != null) {
                val days = (trialExpiry - now).inWholeDays.toInt()

                if (days !in setOf(7, 3, 1)) {
                    return@forEach
                }

                val workspace =
                    workspaceRepository.get(info.workspaceId).getOrNull() ?: return@forEach
                val owner = userRepo.findOwner(workspace.organizationId) ?: return@forEach
                val title = if (days <= 1) {
                    "Your free trial is about to expire."
                } else " Your free trial expires in $days day(s)."

                smsService.send(
                    userId = SYSTEM_ID,
                    organizationId = SYSTEM_ID,
                    workspaceId = SYSTEM_ID,
                    to = owner.phone.value,
                    body = """
                        $title
                        Invoice Ref: ${invoice.referenceCode}
                        Amount: ${invoice.amount.formatted}
                        Please complete payment to avoid service interruption.
                    """.trimIndent()
                )
            }
        }
    }
}
