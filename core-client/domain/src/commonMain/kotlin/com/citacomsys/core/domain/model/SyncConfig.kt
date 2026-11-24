package com.citacomsys.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncTarget(
    val workspaceId: String,
    val branchId: String,
    val isCloudDeployed: Boolean = true
)

@Serializable
data class SyncConfig(
    val isMotherHubEnabled: Boolean = false,
    val maxRetries: Int = 3,
    val syncTimingStrategy: SyncTimingStrategy = SyncTimingStrategy.Auto,
    val preferredNightHour: Int = 2, // 2 AM for night batch sync
    val syncIntervalMinutes: Int = 15,
    val allowCellularSync: Boolean = true,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
    val targets: List<SyncTarget> = emptyList()
)


@Serializable
enum class SyncTimingStrategy {
    Auto,      // Real-time / frequent polling
    NIGHT,      // Batched off-peak sync during night hours
    TIMEBASE    // Scheduled interval timebase
}
