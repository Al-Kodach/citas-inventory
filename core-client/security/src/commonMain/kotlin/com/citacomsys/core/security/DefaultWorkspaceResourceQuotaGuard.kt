package com.citacomsys.core.security

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.security.DataAccessWindow
import com.citacomsys.core.domain.security.HistoryVisibilityStatus
import com.citacomsys.core.domain.security.LedgerAccessWindow
import com.citacomsys.core.domain.security.QuotaStatus
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.ResourceType
import io.github.aakira.napier.Napier
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
internal class DefaultWorkspaceResourceQuotaGuard(
    private val dbManager: InventoryDatabaseManager,
    private val service: PlatformClientService,
    private val session: AppSessionManager,
    private val store: PlatformRuntimeStore
) : WorkspaceResourceQuotaGuard {

    private val TAG = "WS_RESOURCE_QUOTA_GUARD"

    override suspend fun consumeLocalResource(
        resourceType: ResourceType,
        amount: Int,
        workspaceId: String?
    ) {
        val targetWs = workspaceId ?: session.activeWorkspaceId ?: error("Workspace id not found")
        val profile = store.getProfile(targetWs) ?: error("Workspace Resources not found")
        val config = profile.config
        val usage = config.usage

        val now = Clock.System.now()
        val updatedUsage = when (resourceType) {
            ResourceType.PRODUCT -> usage.copy(
                productCountUsed = (usage.productCountUsed + amount).coerceAtLeast(0),
                updatedAt = now
            )

            ResourceType.BATCH -> usage.copy(
                activeBatchesCount = (usage.activeBatchesCount + amount).coerceAtLeast(0),
                updatedAt = now
            )

            ResourceType.USER -> usage.copy(
                userCount = (usage.userCount + amount).coerceAtLeast(0),
                updatedAt = now
            )

            ResourceType.DEVICE -> usage.copy(
                deviceCount = (usage.deviceCount + amount).coerceAtLeast(0),
                updatedAt = now
            )

            ResourceType.BRANCH -> usage.copy(
                branchCount = (usage.branchCount + amount).coerceAtLeast(0),
                updatedAt = now
            )

            ResourceType.SMS,
            ResourceType.PACKAGE -> usage
        }

        val updatedConfig = config.copy(usage = updatedUsage)
        val updatedProfile = profile.copy(config = updatedConfig)

        store.saveWorkstationProfile(updatedProfile)
        store.updateWorkspaceConfig(updatedConfig)

        Napier.i(tag = TAG) {
            "Locally consumed resource $resourceType (amount: $amount) for workspace $workspaceId. Updated usage: $updatedUsage"
        }
    }

    override suspend fun getProductQuotaStatus(): QuotaStatus {
        val resource = service.resource.value ?: return QuotaStatus(0, 0, "Products")
        val used = dbManager.getDatabase().productQueries.count().executeAsOneOrNull()?.toInt() ?: 0
        val limit = if (resource.maxProductsLimit <= 0) 1000 else resource.maxProductsLimit

        return QuotaStatus(
            used = used,
            limit = limit,
            resourceLabel = "Products"
        )
    }

    override fun getLedgerVisibilityStatus(): HistoryVisibilityStatus {
        val resource = service.resource.value!!
        val months = resource.historyRetentionMonths

        return if (months <= 0) {
            HistoryVisibilityStatus(
                visibleMonths = 0,
                isUnrestricted = true,
                label = "Unlimited History"
            )
        } else {
            HistoryVisibilityStatus(
                visibleMonths = months,
                isUnrestricted = false,
                label = "Last $months Months",
                upgradeNudge = if (months < 12) "Increase retention with a higher plan" else null
            )
        }
    }


    override fun getSalesVisibilityStatus(): HistoryVisibilityStatus {
        val resource = service.resource.value!!
        val months = resource.historyRetentionMonths

        return if (months <= 0) {
            HistoryVisibilityStatus(0, true, "All Transactions")
        } else {
            HistoryVisibilityStatus(
                visibleMonths = months,
                isUnrestricted = false,
                label = "Last $months Months",
                upgradeNudge = if (months < 12) "Increase retention with a higher plan" else null
            )
        }
    }

    override suspend fun getBatchQuotaStatus(): QuotaStatus {
        val workspaceId = session.activeWorkspaceId!!
        val resource = service.resource.value!!

        val used =
            dbManager.getDatabase().batchSnapshotQueries.countActiveBatchesForWorkspace(workspaceId).executeAsOneOrNull()
                ?.toInt() ?: 0

        val limit = if (resource.maxActiveBatchesLimit <= 0) 500 else resource.maxActiveBatchesLimit

        return QuotaStatus(
            used = used,
            limit = limit,
            resourceLabel = "Active Batches"
        )
    }

    override suspend fun verifyProductCreationAllowed() {
        val status = getProductQuotaStatus()
        if (status.isExceeded) {
            throw IllegalStateException("Product catalog limit reached (${status.used}/${status.limit}). Please upgrade to add more products.")
        }
    }

    override suspend fun verifyBatchCreationAllowed() {
        val status = getBatchQuotaStatus()
        if (status.isExceeded) {
            throw IllegalStateException("Active batches limit reached (${status.used}/${status.limit}). Please close old batches or upgrade.")
        }
    }

    override fun calculateLedgerWindow(): LedgerAccessWindow {
        val resource = service.resource.value!!
        val months = resource.historyRetentionMonths

        if (months <= 0) {
            return LedgerAccessWindow(
                isUnrestricted = true,
                oldestAllowedTimestamp = null
            )
        }

        val cutoff = Clock.System.now().minus(months, DateTimeUnit.MONTH, TimeZone.UTC)
        return LedgerAccessWindow(
            isUnrestricted = false,
            oldestAllowedTimestamp = cutoff
        )
    }

    override fun calculateActiveSalesWindow(): DataAccessWindow {
        val resource = service.resource.value ?: return DataAccessWindow(
            isUnrestricted = true,
            oldestAllowedTimestamp = null
        )
        val retentionMonths = resource.historyRetentionMonths

        if (retentionMonths <= 0) {
            return DataAccessWindow(isUnrestricted = true, oldestAllowedTimestamp = null)
        }

        val cutoff = Clock.System.now().minusMonths(retentionMonths)
        return DataAccessWindow(
            isUnrestricted = false,
            oldestAllowedTimestamp = cutoff
        )
    }

    private fun Instant.minusMonths(months: Int): Instant {
        return this.minus(months, DateTimeUnit.MONTH, TimeZone.UTC)
    }
}
