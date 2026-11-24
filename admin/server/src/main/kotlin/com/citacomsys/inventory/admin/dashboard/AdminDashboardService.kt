package com.citacomsys.inventory.admin.dashboard

import com.citacomsys.admin.api.dashboard.AdminDashboardOverview
import com.citacomsys.admin.api.dashboard.RecentOnboarding
import com.citacomsys.core.persistence.table.billing.BillingRecordsTable
import com.citacomsys.core.persistence.table.tenant.BranchesTable
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.table.tenant.Organizations
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.table.tenant.WorkspaceSubscriptionsTable
import com.citacomsys.core.persistence.table.user.Users
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.BillingStatus
import com.citacomsys.types.SubscriptionStatus
import com.citacomsys.types.WorkspaceStatus
import com.citacomsys.types.BillingCycle
import com.citacomsys.types.ProductType
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

internal class AdminDashboardService {

    suspend fun getPlatformSummary(): AdminDashboardOverview = dbQuery {
        val totalOrganizations = Organizations.selectAll().count()
        val activeOrganizations = Organizations.selectAll().where { Organizations.status eq "ACTIVE" }.count()
        val totalWorkspaces = Workspaces.selectAll().count()
        val totalBranches = BranchesTable.selectAll().count()

        val totalTenants = totalWorkspaces
        val activeTenants = Workspaces.selectAll().where { Workspaces.status eq WorkspaceStatus.ACTIVE }.count()
        val totalDevices = HardwareRegistry.selectAll().count()
        val totalUsers = Users.selectAll().count()

        val activeSubs = WorkspaceSubscriptionsTable.selectAll()
            .where { WorkspaceSubscriptionsTable.status eq SubscriptionStatus.ACTIVE }
            .toList()

        val mrrMinor = activeSubs.sumOf {
            val amountMinor = it[WorkspaceSubscriptionsTable.recurringAmountMinor]
            val months = it[WorkspaceSubscriptionsTable.billingCycle].months
            if (months in 1..<1000) {
                amountMinor / months
            } else {
                0L
            }
        }

        val totalRevenueMinor = BillingRecordsTable.select(BillingRecordsTable.amountMinor)
            .where { (BillingRecordsTable.status eq BillingStatus.PAID.name) or (BillingRecordsTable.status eq BillingStatus.FULFILLED.name) }
            .sumOf { it[BillingRecordsTable.amountMinor] }

        val activeSubscriptionCount = activeSubs.size.toLong()
        val trialSubscriptionCount = WorkspaceSubscriptionsTable.selectAll()
            .where { WorkspaceSubscriptionsTable.status eq SubscriptionStatus.TRIAL }
            .count()

        val recentOnboardings = Organizations.selectAll()
            .orderBy(Organizations.createdAt, SortOrder.DESC)
            .limit(5)
            .map {
                RecentOnboarding(
                    organizationId = it[Organizations.id].value.toString(),
                    tenantName = it[Organizations.name],
                    businessType = ProductType.RETAIL,
                    onboardedAt = it[Organizations.createdAt].toString()
                )
            }

        AdminDashboardOverview(
            totalOrganizations = totalOrganizations,
            activeOrganizations = activeOrganizations,
            totalWorkspaces = totalWorkspaces,
            totalBranches = totalBranches,
            totalTenants = totalTenants,
            activeTenants = activeTenants,
            totalDevices = totalDevices,
            totalUsers = totalUsers,
            monthlyRecurringRevenueMinor = mrrMinor,
            totalRevenueMinor = totalRevenueMinor,
            activeSubscriptions = activeSubscriptionCount,
            trialSubscriptions = trialSubscriptionCount,
            systemHealthStatus = "OPTIMAL",
            recentOnboardings = recentOnboardings
        )
    }
}
