package com.citacomsys.inventory.api.user.model

import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.User
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserType
import kotlinx.serialization.Serializable

@Serializable
data class PlatformAdmin(
    val id: String,
    val name: String,
    val username: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val role: String,
    val status: UserStatus,
    val activeHardwareId: String?,
    val mfaEnabled: Boolean
) {
    fun toUser(): User {
        return User(
            id = id,
            username = username,
            role = role,
            status = status,
            organizationId = "00000000-0000-0000-0000-000000000000",
            activeHardwareId = activeHardwareId,
            mfaEnabled = mfaEnabled,
            passwordHash = passwordHash,
            phone = PhoneNumber(phone),
            email = email,
            userType = UserType.PLATFORM,
            isPhoneVerified = true,
            name = name
        )
    }
}
