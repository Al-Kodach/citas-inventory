package com.citacomsys.kernel.model

import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.PackageTier
import com.citacomsys.types.ProductModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
@SerialName("WorkspaceResource")
data class WorkspaceResource(
    val packageId: String,
    val name: String,
    val tier: PackageTier,
    val deploymentMode: DeploymentMode = DeploymentMode.CLOUD_SYNCED,
    val modules: Set<ProductModule>,
    // Fixed Capacity Limits
    val maxUsers: Int,
    val maxDevice: Int,
    val maxBranches: Int,
    val maxProductsLimit: Int,
    val maxActiveBatchesLimit: Int,
    // Retention Brackets (3, 6, 12, 24 months)
    val historyRetentionMonths: Int,
    // Lifecycle Metadata
    val updatedAt: Instant
) {
    companion object {
        val EMPTY = WorkspaceResource(
            packageId = "",
            name = "Default",
            tier = PackageTier.STARTER,
            deploymentMode = DeploymentMode.CLOUD_SYNCED,
            modules = emptySet(),
            maxUsers = 0,
            maxDevice = 0,
            maxBranches = 0,
            maxProductsLimit = 0,
            maxActiveBatchesLimit = 0,
            historyRetentionMonths = 0,
            updatedAt = Clock.System.now()
        )
    }
}

@Serializable
@SerialName("WorkspaceResourcesUsage")
data class WorkspaceResourcesUsage(
    val productCountUsed: Int,
    val activeBatchesCount: Int,
    // Core resource trackers
    val userCount: Int,
    val deviceCount: Int,
    val branchCount: Int,
    // Lifecycle Metadata
    val updatedAt: Instant
) {
    companion object {
        val EMPTY = WorkspaceResourcesUsage(
            productCountUsed = 0,
            activeBatchesCount = 0,
            userCount = 0,
            deviceCount = 0,
            branchCount = 0,
            updatedAt = Clock.System.now()
        )
    }
}
