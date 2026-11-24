package com.citacomsys.inventory.api.user.model

import com.citacomsys.types.generateUUID
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserType
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Data class use to create or update user info
 * @param password null for update
 */
@Serializable
data class SaveTenantUserRequest(
    val id: String = generateUUID(), // client side generated id
    val name: String,
    val username: String,
    val wsId: String?,
    val branchId: String?,
    val phone: PhoneNumber,
    val password: String? = null,
    val role: String,
    val capabilities: Set<UserCapability> = emptySet(),
    val email: String? = null,
    val type: UserType = UserType.TENANT,
    val createdAt: Instant = Clock.System.now(), // use for both update n create
)

@Serializable
data class SuspendUserRequest(
    val userId: String,
    val reason: String,
)

@Serializable
data class UpdatePasswordRequest(val newPassword: String)


@Serializable
data class BindUserToWorkspaceRequest(
    val userId: String,
    val workspaceId: String,
    val role: String,
    val branchId: String? = null,
    val capabilities: Set<UserCapability> = emptySet(),
    val useOrgCapabilities: Boolean = true
) {
    val resolvedBranchId: String
        get() = branchId?.takeIf { it.isNotBlank() } ?: workspaceId
}

@Serializable
data class UnbindUserFromWorkspaceRequest(
    val userId: String,
    val workspaceId: String
)
