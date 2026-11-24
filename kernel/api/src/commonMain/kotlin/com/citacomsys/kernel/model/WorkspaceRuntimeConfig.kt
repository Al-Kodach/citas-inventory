package com.citacomsys.kernel.model

import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.ProductType
import com.citacomsys.types.SubscriptionStatus
import com.citacomsys.types.WorkspaceStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the configuration and consumption state for a specific business workspace.
 */
@Serializable
@SerialName("WorkspaceRuntimeConfig")
data class WorkspaceRuntimeConfig(
    val workspaceId: String,
    val name: String,
    val type: ProductType,
    val status: WorkspaceStatus,
    val resource: WorkspaceResource,
    val usage: WorkspaceResourcesUsage,
    val subscriptionStatus: SubscriptionStatus,
    val branding: BrandingConfig?
)