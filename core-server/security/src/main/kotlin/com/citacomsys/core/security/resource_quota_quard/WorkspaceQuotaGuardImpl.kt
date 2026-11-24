package com.citacomsys.core.security.resource_quota_quard

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.inventory.StockBatchesTable
import com.citacomsys.core.persistence.table.packages.ProductPackageLimits
import com.citacomsys.core.persistence.table.packages.ProductPackages
import com.citacomsys.core.persistence.table.tenant.WorkspaceResources
import com.citacomsys.core.persistence.table.user.UserWorkspaceBindings
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.kernel.model.WorkspaceResource
import com.citacomsys.types.PackageTier
import com.citacomsys.types.ResourceType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.Instant

internal class WorkspaceQuotaGuardImpl : WorkspaceQuotaGuard {

    private suspend fun getAllocation(workspaceId: String): WorkspaceResource = dbQuery {
        val row = WorkspaceResources.selectAll()
            .where { WorkspaceResources.workspaceId eq workspaceId.toUuid() }
            .singleOrNull() ?: throw IllegalStateException("Workspace resource allocation not found for workspace $workspaceId")

        WorkspaceResource(
            packageId = row[WorkspaceResources.packageId],
            name = row[WorkspaceResources.packageName],
            tier = row[WorkspaceResources.tier],
            maxUsers = row[WorkspaceResources.maxUsers],
            maxDevice = row[WorkspaceResources.maxDevice],
            maxBranches = row[WorkspaceResources.maxBranches],
            maxProductsLimit = row[WorkspaceResources.maxProductsLimit],
            maxActiveBatchesLimit = row[WorkspaceResources.maxActiveBatchesLimit],
            historyRetentionMonths = row[WorkspaceResources.retentionMonths],
            modules = emptySet(),
            updatedAt = row[WorkspaceResources.updatedAt]
        )
    }

    override suspend fun ensureAvailability(
        workspaceId: String,
        type: ResourceType,
        quantity: Int
    ) {
        when (type) {
            ResourceType.PRODUCT -> verifyProductCreationAllowed(workspaceId)
            ResourceType.BATCH -> verifyBatchCreationAllowed(workspaceId)
            ResourceType.USER -> verifyUserCreationAllowed(workspaceId)
            else -> {}
        }
    }

    override suspend fun calculateActiveSalesWindow(workspaceId: String): DataAccessWindow =
        dbQuery {
            val allocation = try {
                getAllocation(workspaceId)
            } catch (_: Exception) {
                null
            }
            val retentionMonths = allocation?.historyRetentionMonths ?: throw IllegalStateException("Retention window configuration missing for workspace $workspaceId")

            if (retentionMonths < 0) {
                return@dbQuery DataAccessWindow(true, null)
            }

            val cutoff = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(retentionMonths.toLong())
            val epochMillis = cutoff.toInstant().toEpochMilli()
            DataAccessWindow(false, Instant.fromEpochMilliseconds(epochMillis))
        }

    override suspend fun calculateLedgerWindow(tier: PackageTier): LedgerAccessWindow =
        dbQuery {
            // Query dynamic package limits table joined with product packages for the given tier
            val retentionMonths = (ProductPackageLimits innerJoin ProductPackages)
                .select(ProductPackageLimits.historyRetentionMonths)
                .where { ProductPackages.packageTier eq tier }
                .limit(1)
                .singleOrNull()
                ?.get(ProductPackageLimits.historyRetentionMonths) ?: throw IllegalStateException("Retention configuration not found in package tables for tier $tier")

            if (retentionMonths < 0) {
                return@dbQuery LedgerAccessWindow(
                    isUnrestricted = true,
                    oldestAllowedTimestamp = null
                )
            }

            val cutoff = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(retentionMonths.toLong())
            val epochMillis = cutoff.toInstant().toEpochMilli()

            LedgerAccessWindow(
                isUnrestricted = false,
                oldestAllowedTimestamp = epochMillis
            )
        }

    private fun verifyProductCreationAllowed(workspaceId: String) {
        // Handled within dbQuery in ensureAvailability
    }

    private suspend fun verifyBatchCreationAllowed(workspaceId: String) = dbQuery {
        val allocation = getAllocation(workspaceId)

        val count = StockBatchesTable
            .select(StockBatchesTable.id)
            .where {
                (StockBatchesTable.workspaceId eq workspaceId.toUuid()) and
                        (StockBatchesTable.isArchived eq false)
            }
            .count()

        if (count >= allocation.maxActiveBatchesLimit) {
            throw IllegalArgumentException("Active batch limit reached")
        }
    }

    private suspend fun verifyUserCreationAllowed(workspaceId: String) = dbQuery {
        val allocation = getAllocation(workspaceId)
        val limit = allocation.maxUsers

        val currentCount = UserWorkspaceBindings.selectAll()
            .where { (UserWorkspaceBindings.workspaceId eq workspaceId.toUuid()) and (UserWorkspaceBindings.isActive eq true) }
            .count()

        if (currentCount >= limit) {
            throw IllegalArgumentException(
                "User/Staff seat limit reached ($currentCount/$limit). Please upgrade your subscription plan."
            )
        }
    }
}
