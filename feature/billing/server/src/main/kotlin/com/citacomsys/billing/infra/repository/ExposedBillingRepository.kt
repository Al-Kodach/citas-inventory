package com.citacomsys.billing.infra.repository

import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.api.billing.BillingRepository
import com.citacomsys.api.billing.BillingSettings
import com.citacomsys.api.billing.PaymentSubmission
import com.citacomsys.core.persistence.table.billing.BillingRecordsTable
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.tenant.WorkspaceSubscriptionsTable
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.BillingStatus
import com.citacomsys.types.BillingType
import com.citacomsys.types.Money
import com.citacomsys.types.WorkspaceStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.notExists
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock
import kotlin.time.Instant

internal class ExposedBillingRepository : BillingRepository {

    private fun ResultRow.toBillRecord() = BillingRecord(
        id = this[BillingRecordsTable.id].toString(),
        workspaceId = this[BillingRecordsTable.workspaceId].toString(),
        wsName = this[Workspaces.name],

        type = BillingType.valueOf(this[BillingRecordsTable.type]),
        referenceCode = this[BillingRecordsTable.referenceCode],
        amount = Money(
            this[BillingRecordsTable.amountMinor],
            this[BillingRecordsTable.currency]
        ),
        status = BillingStatus.valueOf(this[BillingRecordsTable.status]),
        summary = this[BillingRecordsTable.summary],
        quote = kotlinx.serialization.json.Json.decodeFromString(this[BillingRecordsTable.quote]),

        paymentChannel = com.citacomsys.api.billing.PaymentChannel.valueOf(this[BillingRecordsTable.paymentChannel]),
        paymentTarget = this[BillingRecordsTable.paymentTarget],
        paymentRecipientName = this[BillingRecordsTable.paymentRecipientName],
        paymentSubmission = this[BillingRecordsTable.paymentSubmission]?.let { 
             kotlinx.serialization.json.Json.decodeFromString(it) 
        },

        origin = com.citacomsys.types.BillingOrigin.valueOf(this[BillingRecordsTable.origin]),
        reason = com.citacomsys.types.BillingReason.valueOf(this[BillingRecordsTable.reason]),

        fulfillmentAttempts = this[BillingRecordsTable.fulfillmentAttempts],
        lastFulfillmentError = this[BillingRecordsTable.lastFulfillmentError],
        fulfilledAt = this[BillingRecordsTable.fulfilledAt],

        createdBy = this[BillingRecordsTable.createdBy] ?: "SYSTEM",
        verifiedBy = this[BillingRecordsTable.verifiedBy],

        createdAt = this[BillingRecordsTable.createdAt],
        updatedAt = this[BillingRecordsTable.updatedAt],
        expiresAt = this[BillingRecordsTable.expiresAt],

        subscriptionVersion = this[BillingRecordsTable.subscriptionVersion]?.toLong()
    )


    override suspend fun save(invoice: BillingRecord): Unit = dbQuery {
        BillingRecordsTable.upsert {
            it[id] = invoice.id.toUuid()
            it[workspaceId] = invoice.workspaceId.toUuid()
            it[type] = invoice.type.name
            it[amountMinor] = invoice.amount.amountMinor
            it[currency] = invoice.amount.currencyCode
            it[status] = invoice.status.name
            it[referenceCode] = invoice.referenceCode
            it[summary] = invoice.summary

            it[paymentChannel] = invoice.paymentChannel.name
            it[paymentRecipientName] = invoice.paymentRecipientName
            it[paymentTarget] = invoice.paymentTarget

            it[origin] = invoice.origin.name
            it[reason] = invoice.reason.name
            it[quote] = kotlinx.serialization.json.Json.encodeToString(invoice.quote)

            it[createdBy] = invoice.createdBy
            it[verifiedBy] = invoice.verifiedBy

            it[createdAt] = invoice.createdAt
            it[updatedAt] = invoice.updatedAt
            it[expiresAt] = invoice.expiresAt

            it[subscriptionVersion] = invoice.subscriptionVersion?.toInt()
        }
    }

    override suspend fun fetch(): List<BillingRecord> = dbQuery {
        BillingRecordsTable.innerJoin(Workspaces).selectAll()
            .limit(100)
            .orderBy(BillingRecordsTable.createdAt, SortOrder.DESC)
            .map { it.toBillRecord() }
    }

    override suspend fun updateStatus(invoiceId: String, status: BillingStatus) {
        dbQuery {
            BillingRecordsTable.update({ BillingRecordsTable.id eq invoiceId.toUuid() }) {
                it[BillingRecordsTable.status] = status.name
                it[expiresAt] = null
            }
        }
    }

    override suspend fun updateFulfilmentDetails(
        invoiceId: String,
        attempts: Int,
        error: String?,
        fulfilledAt: Instant?,
    ) {
        dbQuery {
            BillingRecordsTable.update({ BillingRecordsTable.id eq invoiceId.toUuid() }) {
                it[fulfillmentAttempts] = attempts
                it[lastFulfillmentError] = error
                it[this.fulfilledAt] = fulfilledAt

                if (fulfilledAt != null) {
                    it[status] = BillingStatus.FULFILLED.name
                }
            }
        }
    }

    override suspend fun findPendingSubscriptionInvoice(
        workspaceId: String,
        version: Long,
        type: BillingType
    ): BillingRecord? = dbQuery {
        BillingRecordsTable.innerJoin(Workspaces).selectAll().where {
            (BillingRecordsTable.workspaceId eq workspaceId.toUuid()) and
                    (BillingRecordsTable.subscriptionVersion eq version.toInt()) and
                    (BillingRecordsTable.type eq type.name) and
                    (BillingRecordsTable.status inList listOf(
                        BillingStatus.PENDING.name, BillingStatus.PAYMENT_SUBMITTED.name, BillingStatus.PAID.name
                    ))
        }
            .singleOrNull()?.toBillRecord()
    }

    override suspend fun find(id: String): BillingRecord? = dbQuery {
        BillingRecordsTable.innerJoin(Workspaces).selectAll().where { BillingRecordsTable.id eq id.toUuid() }
            .singleOrNull()
            ?.toBillRecord()
    }

    override suspend fun findByReference(reference: String): BillingRecord? = dbQuery {
        BillingRecordsTable.innerJoin(Workspaces).selectAll().where { BillingRecordsTable.referenceCode eq reference }
            .singleOrNull()
            ?.toBillRecord()
    }

    override suspend fun findByTenant(workspaceId: String): List<BillingRecord> = dbQuery {
        BillingRecordsTable.innerJoin(Workspaces).selectAll().where { BillingRecordsTable.workspaceId eq workspaceId.toUuid() }
            .map { it.toBillRecord() }
    }

    override suspend fun findActiveByTenant(workspaceId: String): List<BillingRecord> = dbQuery {
        BillingRecordsTable.innerJoin(Workspaces).selectAll().where {
            (BillingRecordsTable.workspaceId eq workspaceId.toUuid()) and
                    ((BillingRecordsTable.status eq BillingStatus.PENDING.name) or
                            (BillingRecordsTable.status eq BillingStatus.PAYMENT_SUBMITTED.name))
        }.map { it.toBillRecord() }
    }

    override suspend fun listPendingVerification(): List<BillingRecord> = dbQuery {
        BillingRecordsTable.innerJoin(Workspaces).selectAll().where { BillingRecordsTable.status eq BillingStatus.PAYMENT_SUBMITTED.name }
            .map { it.toBillRecord() }
    }

    override suspend fun saveSubmission(submission: PaymentSubmission) {
        dbQuery {
            BillingRecordsTable.update({ BillingRecordsTable.id eq submission.invoiceId.toUuid() }) {
                it[paymentSubmission] = kotlinx.serialization.json.Json.encodeToString(submission)
                it[status] = BillingStatus.PAYMENT_SUBMITTED.name
                it[updatedAt] = submission.submittedAt
                it[expiresAt] = null
            }
        }
    }

    override suspend fun findTenantsNeedingRenewal(horizon: Instant): List<String> = dbQuery {
        WorkspaceSubscriptionsTable.innerJoin(Workspaces).leftJoin(BillingRecordsTable)
            .select(WorkspaceSubscriptionsTable.workspaceId).where {
                WorkspaceSubscriptionsTable.endedAt lessEq horizon and
                        (Workspaces.status eq WorkspaceStatus.ACTIVE) and
                notExists(
                    BillingRecordsTable.select(BillingRecordsTable.id)
                        .where {
                            (BillingRecordsTable.workspaceId eq WorkspaceSubscriptionsTable.workspaceId) and
                                    ((BillingRecordsTable.status eq BillingStatus.PENDING.name) or
                                            (BillingRecordsTable.status eq BillingStatus.PAYMENT_SUBMITTED.name))
                        }
                )
            }
            .map { it[WorkspaceSubscriptionsTable.workspaceId].toString() }
    }

    override suspend fun expireInvoices(now: Instant): Int = dbQuery {
        BillingRecordsTable.update({
            (BillingRecordsTable.status eq BillingStatus.PENDING.name) and
                    (BillingRecordsTable.expiresAt less now)
        }) {
            it[status] = BillingStatus.EXPIRED.name
            it[updatedAt] = now
        }
    }

    override suspend fun expireSubscriptionInvoices(
        workspaceId: String,
        maxVersion: Long
    ): Int {
        return dbQuery {
            BillingRecordsTable.update({
                (BillingRecordsTable.workspaceId eq workspaceId.toUuid()) and
                        (BillingRecordsTable.subscriptionVersion lessEq maxVersion.toInt()) and
                        (BillingRecordsTable.status eq BillingStatus.PENDING.name)
            }) {
                it[status] = BillingStatus.EXPIRED.name
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun findTrialConversionInvoice(workspaceId: String): BillingRecord? {
        return dbQuery {
            BillingRecordsTable.innerJoin(Workspaces).selectAll()
                .where {
                    (BillingRecordsTable.workspaceId eq workspaceId.toUuid()) and
                            (BillingRecordsTable.type eq BillingType.TRIAL_CONVERSION.name) and
                            (BillingRecordsTable.status eq BillingStatus.PENDING.name)
                }
                .singleOrNull()?.toBillRecord()
        }
    }

    override suspend fun getSettings(): BillingSettings {
        return BillingSettings()
    }
}
