package com.citacomsys.inventory.api.user

import com.citacomsys.types.UserCapability
import kotlinx.serialization.Serializable

interface RoleRepository {
    suspend fun save(
        orgId: String,
        role: String,
        capabilities: Set<UserCapability>
    ): String

    suspend fun findCapabilities(orgId: String, role: String): Set<UserCapability>

    suspend fun findAllCapabilities(orgId: String): Map<String, Set<UserCapability>>

    suspend fun saveInheritance(
        orgId: String,
        role: String,
        parentRole: String
    ): Result<Unit>

    suspend fun deleteInheritance(
        orgId: String,
        role: String,
        parentRole: String
    ): Result<Unit>

    suspend fun findInheritance(orgId: String): List<RoleInheritanceDto>
}

@Serializable
data class RoleInheritanceDto(
    val roleName: String,
    val parentRoleName: String
)
