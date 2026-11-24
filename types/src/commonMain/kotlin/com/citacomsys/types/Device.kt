package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * [TECH MODE] - Managed by Hub Admins.
 * Raw physical node identity and global security standing.
 */
@Serializable
data class HardwareTechInfo(
    val hardwareId: String,
    val fingerprint: String?,
    val model: String?,
    val osName: String?,
    val displayName: String? = null,
    val architecture: String?,
    val appFlavor: String? = null,
    val masterStatus: HardwareMasterStatus = HardwareMasterStatus.ACTIVE,
    val securityReason: String? = null,
    val updatedAt: Instant
)

@Serializable
enum class HardwareMasterStatus {
    ACTIVE,
    REVOKED // Global blacklist - denied across all nodes including Hub
}

/**
 * [USER MODE] - Visible to User.
 * Friendly industrial workstation identity.
 */
@Serializable
data class UserDeviceInfo(
    val hardwareId: String,
    val status: DeviceStatus,
    val displayName: String,
    val lastUsedAt: Instant,
    val lastIp: String? = null
)

/**
 * [USER MODE] - Visible to User/Admin/OrgMode.
 * Friendly industrial workstation identity and active binding state.
 */
@Serializable
@SerialName("DeviceSummary")
data class DeviceSummary(
    val hardwareId: String,
    val name: String,
    val model: String? = null,
    val osName: String? = null,
    val masterStatus: HardwareMasterStatus = HardwareMasterStatus.ACTIVE,
    val lastIp: String? = null,
    val lastUsedAt: Instant,

    // All workstations this hardware is currently bound to
    val bindings: List<WorkstationBinding> = emptyList(),
)

@Serializable
data class WorkstationBinding(
    val bindingId: String,
    val workspaceName: String,
    val workspaceId: String,
    val status: DeviceStatus,
    val boundAt: Instant,
    val lastIp: String? = null,
    val lastUsedAt: Instant,
)

enum class DeviceStatus {
    ACTIVE,
    LOCKED,
    REVOKED;

    fun revoked(): Boolean = this == REVOKED
    fun locked(): Boolean = this == LOCKED
    fun trusted(): Boolean = this == ACTIVE

    val toMasterStatus: HardwareMasterStatus
        get() = when (this) {
            LOCKED -> HardwareMasterStatus.REVOKED
            REVOKED -> HardwareMasterStatus.REVOKED
            else -> HardwareMasterStatus.ACTIVE
        }
}

/**
 * Unified context for internal security decisions.
 * Contains ONLY the specific fields required for hardware security checks.
 */
@Serializable
data class WorkstationSecurityContext(
    val hardwareId: String,
    val fingerprint: String? = null,
    val masterStatus: HardwareMasterStatus = HardwareMasterStatus.ACTIVE,
    val bindingStatus: Map<String, DeviceStatus>? = null
)

/**
 * [MIN MODEL] - Internal Kernel use.
 * Lightweight state for security gates and enforcement policy generation.
 */
@Serializable
data class WorkstationEnforcementState(
    val hardwareId: String,
    val workspaceId: String?,
    val organizationId: String,
    val isGloballyLocked: Boolean,
    val isBindingTrusted: Boolean,
    val bindingStatus: DeviceStatus
)

@Serializable
data class WorkspaceClaimRequest(
    val workspaceId: String,
    val deviceName: String,
    val techInfo: HardwareTechInfo? = null
)

@Serializable
data class NodeRuntimeConfig(
    val currentDeploymentMode: String,
    val nodeId: String,
    val name: String
)