package com.citacomsys.admin.api.dashboard

import com.citacomsys.types.ProductType
import kotlinx.serialization.Serializable

@Serializable
data class AdminDashboardOverview(
    val totalOrganizations: Long = 0,
    val activeOrganizations: Long = 0,
    val totalWorkspaces: Long = 0,
    val totalBranches: Long = 0,
    val totalTenants: Long = 0,
    val activeTenants: Long = 0,
    val totalDevices: Long = 0,
    val totalUsers: Long = 0,
    val monthlyRecurringRevenueMinor: Long = 0,
    val totalRevenueMinor: Long = 0,
    val activeSubscriptions: Long = 0,
    val trialSubscriptions: Long = 0,
    val systemHealthStatus: String = "OPTIMAL",
    val recentOnboardings: List<RecentOnboarding> = emptyList()
)

@Serializable
data class RecentOnboarding(
    val organizationId: String,
    val tenantName: String,
    val businessType: ProductType,
    val onboardedAt: String
)
