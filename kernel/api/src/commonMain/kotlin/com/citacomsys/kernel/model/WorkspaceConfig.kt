package com.citacomsys.kernel.model

import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.PackageTier
import com.citacomsys.types.ProductType
import kotlinx.serialization.Serializable

/**
 * Bootstrap configuration for a specific industry workspace.
 */
@Serializable
data class WorkspaceConfig(
    val organizationId: String,
    val workspaceId: String,
    val packageTier: PackageTier,
    val appType: ProductType,
    val workspaceName: String,
    val productKey: String,
    val deploymentMode: DeploymentMode,
    val branding: BrandingConfig?,
    val timezone: String? = null,
    val currencyCode: String? = null,
    val countryCode: String? = null,
    val version: Int = 0
) {
    val isCloudDeployed: Boolean
        get() = deploymentMode.isCloudSynced
}
