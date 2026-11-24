package com.citacomsys.test

import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserType
import kotlin.time.Clock

object TestDataFactory {
    fun createUser(
        id: String = "test-user-id",
        name: String = "Test User",
        username: String = "testuser",
        organizationId: String = "00000000-0000-0000-0000-000000000001",
        role: String = "ADMIN",
        status: UserStatus = UserStatus.ACTIVE,
        userType: UserType = UserType.TENANT,
        capabilities: Set<UserCapability> = UserCapability.entries.toSet()
    ) = User(
        id = id,
        name = name,
        username = username,
        organizationId = organizationId,
        phone = PhoneNumber("0240000000"),
        isPhoneVerified = true,
        email = "test@example.com",
        passwordHash = "hashed",
        userType = userType,
        mfaEnabled = false,
        status = status,
        role = role,
        capabilities = capabilities,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    val adminUser = createUser()
}
