package com.citacomsys.inventory.api.user

import com.citacomsys.inventory.api.user.model.PlatformAdmin
import com.citacomsys.types.UserStatus

interface PlatformAdminRepository {
    suspend fun findById(id: String): Result<PlatformAdmin>
    suspend fun findByIdentifier(identity: String): Result<PlatformAdmin>
    suspend fun updateActiveHardware(adminId: String, hardwareId: String)
    suspend fun updateStatus(adminId: String, status: UserStatus)
}
