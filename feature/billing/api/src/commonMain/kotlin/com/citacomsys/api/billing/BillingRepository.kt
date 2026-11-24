package com.citacomsys.api.billing

import com.citacomsys.types.BillingStatus
import com.citacomsys.types.BillingType
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import kotlin.time.Instant

interface BillingRepository {

    suspend fun save(invoice: BillingRecord)

    suspend fun fetch(): List<BillingRecord>

    suspend fun find(id: String): BillingRecord?

    suspend fun updateStatus(invoiceId: String, status: BillingStatus)

    suspend fun updateFulfilmentDetails(
        invoiceId: String,
        attempts: Int,
        error: String?,
        fulfilledAt: Instant?
    )

    suspend fun findPendingSubscriptionInvoice(
        workspaceId: String,
        version: Long,
        type: BillingType
    ): BillingRecord?

    suspend fun findByReference(reference: String): BillingRecord?

    suspend fun findByTenant(workspaceId: String): List<BillingRecord>

    suspend fun findActiveByTenant(workspaceId: String): List<BillingRecord>

    suspend fun listPendingVerification(): List<BillingRecord>

    suspend fun saveSubmission(submission: PaymentSubmission)

    suspend fun findTenantsNeedingRenewal(horizon: Instant): List<String>

    suspend fun expireInvoices(now: Instant): Int
    suspend fun expireSubscriptionInvoices(workspaceId: String, maxVersion: Long): Int

    suspend fun findTrialConversionInvoice(workspaceId: String): BillingRecord?

    suspend fun getSettings(): BillingSettings
}
