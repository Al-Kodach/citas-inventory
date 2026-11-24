package com.citacomsys.api.device.node

import com.citacomsys.types.*
import kotlinx.serialization.Serializable

/**
 * Manages the security lifecycle of physical hardware nodes and their
 * workstation bindings within industrial workspaces.
 */
interface DeviceRepository {

    // --- Tech Mode: Hardware Registry (Global Organization Hub) ---

    suspend fun getHardware(orgId: String, hardwareId: String): HardwareTechInfo?

    suspend fun registerHardware(orgId: String, info: HardwareTechInfo)

    suspend fun updateHardwareStatus(
        orgId: String,
        hardwareId: String,
        status: HardwareMasterStatus,
        reason: String?,
        lastIp: String? = null,
    )

    suspend fun getDeviceInfo(orgId: String, hardwareId: String): UserDeviceInfo?
    suspend fun getAllDevicesForOrg(orgId: String): List<UserDeviceInfo>

    suspend fun getDeviceSummary(orgId: String, hardwareId: String): DeviceSummary?

    // --- User Mode: Workstation Bindings (Industrial Workspace) ---
    suspend fun getBindings(
        orgId: String,
        identity: String, // hardwareId or workspaceId
        hardwareId: String
    ): List<WorkstationBinding>

    suspend fun getBinding(orgId: String, bindingId: String): WorkstationBinding?

    suspend fun saveBinding(
        orgId: String,
        hardwareId: String,
        binding: WorkstationBinding
    )

    suspend fun updateDeviceInfo(orgId: String, deviceInfo: UserDeviceInfo)

    suspend fun updateBindingUsage(
        orgId: String,
        bindingId: String,
        ip: String
    )

    suspend fun revokeBinding(orgId: String, bindingId: String)

    suspend fun relinkBinding(orgId: String, bindingId: String)

    suspend fun listBindings(orgId: String, workspaceId: String): List<UserDeviceInfo>

    /**
     * Retrieves the complete security context for a workstation.
     */
    suspend fun getSecurityContext(
        orgId: String,
        workspaceId: String,
        hardwareId: String
    ): WorkstationSecurityContext
}

@Serializable
data class WorkstationSecurityContext(
    val tech: HardwareTechInfo,
    val userView: UserDeviceInfo? = null
)
