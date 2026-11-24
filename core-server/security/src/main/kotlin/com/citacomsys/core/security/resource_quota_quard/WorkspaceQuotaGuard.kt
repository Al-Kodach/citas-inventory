package com.citacomsys.core.security.resource_quota_quard

import com.citacomsys.types.PackageTier
import com.citacomsys.types.ResourceType
import kotlin.time.Instant

interface WorkspaceQuotaGuard {
    /**
     * Verifies that the workspace has sufficient remaining quota or balance for the operation.
     * Throws SecurityException if limit exceeded.
     */
    suspend fun ensureAvailability(workspaceId: String, type: ResourceType, quantity: Int = 1)

    /**
     * Calculates the visibility window for Sales/Transactions based on Tier.
     */
    suspend fun calculateActiveSalesWindow(workspaceId: String): DataAccessWindow

    /**
     * Precise calendar-month based window for the Financial Ledger.
     */
    suspend fun calculateLedgerWindow(tier: PackageTier): LedgerAccessWindow
}

data class DataAccessWindow(
    val isUnrestricted: Boolean,
    val oldestAllowedInstant: Instant?
)

data class LedgerAccessWindow(
    val isUnrestricted: Boolean,
    val oldestAllowedTimestamp: Long?
)
