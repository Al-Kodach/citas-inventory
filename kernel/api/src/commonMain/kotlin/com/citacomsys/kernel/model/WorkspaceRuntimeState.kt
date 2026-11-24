package com.citacomsys.kernel.model

import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.WorkspaceStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("WorkspaceRuntimeState")
data class WorkspaceRuntimeState(
    val workspaceId: String,
    val workspaceStatus: WorkspaceStatus,
    val deviceStatus: DeviceStatus,
    val enforcement: WorkspaceEnforcementPolicy,
    val runtimeFlags: RuntimeFlags,
    val productKey: String = "",
    val configVersion: Int,
    val serverTime: Instant
)

@Serializable
@SerialName("RuntimeFlags")
data class RuntimeFlags(
    val hasPendingInvoice: Boolean = false,
    val showSubscriptionWarning: Boolean = false,
    val showPaymentBanner: Boolean = false,
    val maintenanceMode: Boolean = false,
)
