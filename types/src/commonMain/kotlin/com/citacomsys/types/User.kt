package com.citacomsys.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Instant
import kotlin.time.Clock

@Serializable
data class User(
    val id: String,
    val organizationId: String,
    val name: String,
    val username: String,
    val phone: PhoneNumber,
    val isPhoneVerified: Boolean,
    val email: String?,
    @Transient val passwordHash: String = "",
    val activeHardwareId: String? = null, // For admins
    val userType: UserType,
    val mfaEnabled: Boolean,
    val status: UserStatus,
    val role: String,
    val capabilities: Set<UserCapability> = emptySet(),
    val suspendedReason: String? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val deletedAt: Instant? = null
) {
    val phoneMask = phone.mask
    val emailMask = email?.let {
        val parts = email.split("@")
        if (parts.size == 2 && parts[0].length > 2) {
            parts[0].take(2) + "***@" + parts[1]
        } else email
    }
}

@Serializable
enum class UserType {
    SYSTEM,
    PLATFORM,
    TENANT
}

@Serializable
data class UserItem(
    val id: String,
    val name: String,
    val username: String,
    val emailMasked: String? = null,
    val phoneMasked: String? = null,
    val status: UserStatus,
    val role: String,
    val createdAt: String
) {
    val emailMask = emailMasked
    val phoneMask = phoneMasked
}