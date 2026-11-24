package com.citacomsys.kernel.model

import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.ProductType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Encapsulates the complete authorized state for a workstation.
 * Persisted locally to allow offline context switching and last-active restoration.
 */
@Serializable
data class LocalWorkstationProfile(
    val workspaceId: String,
    val organizationId: String,
    val branchId: String,
    val branchName: String,
    val appType: ProductType,
    val deviceStatus: DeviceStatus = DeviceStatus.ACTIVE,
    val config: WorkspaceRuntimeConfig,
    val state: WorkspaceRuntimeState,
    val lastActivatedAt: Instant
)
