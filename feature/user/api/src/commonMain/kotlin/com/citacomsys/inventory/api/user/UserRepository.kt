package com.citacomsys.inventory.api.user

import com.citacomsys.inventory.api.user.model.BindUserToWorkspaceRequest
import com.citacomsys.inventory.api.user.model.SaveTenantUserRequest
import com.citacomsys.inventory.api.user.model.UnbindUserFromWorkspaceRequest
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.SessionItemDto
import com.citacomsys.types.User
import com.citacomsys.types.UserListItem
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserWorkspaceBinding
import kotlin.time.Instant

interface UserRepository {
    suspend fun save(user: User): User
    suspend fun save(request: SaveTenantUserRequest, organizationId: String): User
    suspend fun findById(id: String): Result<User>

    suspend fun findByPhone(phone: String): Result<User>

    suspend fun suspendUser(userId: String, reason: String): Result<User>

    suspend fun activateUser(userId: String): Result<User>
    suspend fun markPhoneNumberAsVerified(userId: String)
    suspend fun findOwner(organizationId: String): User?

    suspend fun getChangesSince(
        since: Instant?,
        organizationId: String
    ): ChangeListResponse<User, String>

    suspend fun updatePassword(
        userId: String,
        newPasswordHash: String,
        forceChange: Boolean = false
    ): Boolean

    suspend fun updateStatus(status: UserStatus, userId: String)

    suspend fun delete(userId: String, actorId: String): Result<Unit>
    suspend fun updateMfaEnabled(id: String, bool: Boolean)
    suspend fun findSuspendedUsers(organizationId: String): List<User>
    suspend fun findByUsername(identity: String): Result<User>
    suspend fun findByIdentity(identity: String): Result<User>

    suspend fun getAdminEmails(organizationId: String): List<String>
    suspend fun fetchPaged(query: PageQuery, organizationId: String): PageResult<UserListItem>
    suspend fun getActiveSessions(organizationId: String): List<SessionItemDto>
    suspend fun getBindingsForUserId(
        userId: String,
        organizationId: String
    ): List<UserWorkspaceBinding>

    suspend fun bindUserToWorkspace(
        request: BindUserToWorkspaceRequest,
        organizationId: String
    ): Result<Unit>

    suspend fun unbindUserFromWorkspace(
        request: UnbindUserFromWorkspaceRequest,
        organizationId: String
    ): Result<Unit>
}
