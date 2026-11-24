package com.citacomsys.kernel.model

import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.Money
import com.citacomsys.types.PackageTier
import com.citacomsys.types.ProductType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * High-level configuration and financial rollup for the parent company.
 */
@Serializable
@SerialName("OrganizationRuntimeConfig")
data class OrganizationRuntimeConfig(
    val organizationId: String,
    val name: String,
    val businessCode: String,
    val fleetDna: String,
    val status: String,

    // Aggregated Metrics
    val metrics: OrganizationMetrics,
    val branding: BrandingConfig?,

    // Active Workspaces (Industry Platforms)
    val workspaces: List<WorkspaceSummary> = emptyList()
)

@Serializable
data class OrganizationMetrics(
    val totalBalance: String,
    val totalWorkstations: Int,
    val totalActiveBranches: Int,
    val totalStaffAccounts: Int,
    val totalMonthlyRecurring: String,
    val totalBilledToDate: String
) {

    companion object {
        val EMPTY = OrganizationMetrics(
            "0",
            0,
            0,
            0,
            Money.zero().formatted,
            Money.zero().formatted
        )
    }
}

@Serializable
data class BranchSummary(
    val id: String,
    val name: String = "Main",
    val locationCode: String = "MAIN",
    val isActive: Boolean = true
)

@Serializable
data class WorkspaceSummary(
    val id: String,
    val name: String,
    val type: ProductType,
    val packageTier: PackageTier,
    val deploymentMode: DeploymentMode,
    val status: String,
    val branches: List<BranchSummary> = emptyList()
) {
    val isCloudDeployed: Boolean
        get() = deploymentMode.isCloudSynced
}
