package com.citacomsys.api.device.service

import com.citacomsys.api.auth.WorkstationSession
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.UserDeviceInfo
import com.citacomsys.types.WorkstationBinding

interface DeviceApiService {
    suspend fun getDevices(): List<UserDeviceInfo>
    suspend fun getDeviceSummary(hardwareId: String): DeviceSummary
    suspend fun getDeviceBindings(hardwareId: String): List<WorkstationBinding>
    suspend fun getDeviceSessions(hardwareId: String): List<WorkstationSession>
    suspend fun renameDevice(
        hardwareId: String,
        newName: String
    ): Result<Unit>

    suspend fun revokeBinding(bindingId: String): Result<Unit>
    suspend fun unbindWorkstation(bindingId: String): Result<Unit>
    suspend fun relinkWorkstation(bindingId: String): Result<Unit>
    suspend fun revokeDeviceGlobally(hardwareId: String): Result<Unit>
    suspend fun revokeSession(sessionId: String): Result<Unit>
}
