package com.citacomsys.user.data

import com.citacomsys.inventory.api.user.model.BindUserToWorkspaceRequest
import com.citacomsys.inventory.api.user.model.SaveTenantUserRequest
import com.citacomsys.inventory.api.user.model.SuspendUserRequest
import com.citacomsys.inventory.api.user.model.UnbindUserFromWorkspaceRequest
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.User
import com.citacomsys.types.UserItem
import com.citacomsys.types.UserWorkspaceBinding

interface ClientUserRepository {
    suspend fun save(request: SaveTenantUserRequest)
    suspend fun bindUserToWorkspace(request: BindUserToWorkspaceRequest): Result<Unit>
    suspend fun unbindUserFromWorkspace(request: UnbindUserFromWorkspaceRequest): Result<Unit>

    suspend fun findByIdRemote(id: String): Result<User>

    suspend fun suspendUser(request: SuspendUserRequest): Result<Unit>
    suspend fun activate(userId: String): Result<Unit>
    suspend fun delete(userId: String): Result<Unit>

    suspend fun getWorkspaceBindingsForUser(userId: String): Result<List<UserWorkspaceBinding>>
    suspend fun syncCloudPage(query: PageQuery): PageResult<UserItem>
}
