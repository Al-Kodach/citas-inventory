package com.citacomsys.core.domain.security

import com.citacomsys.types.ResourceType
import kotlin.time.Instant

data class QuotaStatus(
    val used: Int,
    val limit: Int,
    val resourceLabel: String,
) {
    val remaining: Int = (limit - used).coerceAtLeast(0)
    val isExceeded: Boolean = used >= limit
    val percentage: Float = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 1f

    // UI Helper
    val displayLabel: String = "$used / $limit $resourceLabel"
}

/**
 * UI-focused status for historical data visibility
 */
data class HistoryVisibilityStatus(
    val visibleMonths: Int,
    val isUnrestricted: Boolean,
    val label: String,
    val upgradeNudge: String? = null
)

data class DataAccessWindow(
    val isUnrestricted: Boolean,
    val oldestAllowedTimestamp: Instant?
)

data class LedgerAccessWindow(
    val isUnrestricted: Boolean,
    val oldestAllowedTimestamp: Instant?
)

interface WorkspaceResourceQuotaGuard {
    // Window Logic (Visibility)
    fun calculateLedgerWindow(): LedgerAccessWindow
    fun calculateActiveSalesWindow(): DataAccessWindow

    // UI Info Logic
    suspend fun getProductQuotaStatus(): QuotaStatus
    suspend fun getBatchQuotaStatus(): QuotaStatus

    fun getLedgerVisibilityStatus(): HistoryVisibilityStatus
    fun getSalesVisibilityStatus(): HistoryVisibilityStatus

    // Hard Enforcement Logic
    suspend fun verifyProductCreationAllowed()
    suspend fun verifyBatchCreationAllowed()

    // Local Resource Consumption Method
    suspend fun consumeLocalResource(
        resourceType: ResourceType,
        amount: Int = 1,
        workspaceId: String? = null,
    )
}
