package com.citacomsys.user.data

import com.citacomsys.database.AppUserEntity
import com.citacomsys.types.DefaultCapabilities
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import kotlinx.serialization.json.Json

fun AppUserEntity.toDomain(): User {
    val userRole = role
    val parsedCaps = capabilities.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching { Json.decodeFromString<Set<UserCapability>>(raw) }.getOrNull()
    } ?: emptySet()

    val resolvedCaps = if (parsedCaps.isNotEmpty()) parsedCaps else DefaultCapabilities.forRole(userRole)

    return User(
        id = id,
        name = name,
        email = email_masked,
        username = username,
        phone = PhoneNumber(phone_masked ?: ""),
        organizationId = organization_id,
        userType = user_type,
        mfaEnabled = false,
        isPhoneVerified = false,
        role = userRole,
        capabilities = resolvedCaps,
        status = status,
        suspendedReason = suspendReason,
        createdAt = created_at,
        updatedAt = last_updated_at,
        deletedAt = null,
        passwordHash = ""
    )
}
