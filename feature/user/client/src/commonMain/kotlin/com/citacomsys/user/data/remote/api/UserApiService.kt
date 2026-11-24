package com.citacomsys.user.data.remote.api

import com.citacomsys.inventory.api.user.model.BindUserToWorkspaceRequest
import com.citacomsys.inventory.api.user.model.SaveTenantUserRequest
import com.citacomsys.inventory.api.user.model.SuspendUserRequest
import com.citacomsys.inventory.api.user.model.UnbindUserFromWorkspaceRequest
import com.citacomsys.inventory.api.user.model.UpdatePasswordRequest
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.User
import com.citacomsys.types.UserItem
import com.citacomsys.types.UserWorkspaceBinding

interface UserApiService {
    suspend fun save(request: SaveTenantUserRequest)
    suspend fun bindUserToWorkspace(request: BindUserToWorkspaceRequest)
    suspend fun unbindUserFromWorkspace(request: UnbindUserFromWorkspaceRequest)
    suspend fun suspend(userId: String, request: SuspendUserRequest)
    suspend fun activate(userId: String)
    suspend fun updatePassword(id: String, request: UpdatePasswordRequest)
    suspend fun getUserWorkspaceBindings(userId: String): List<UserWorkspaceBinding>
    suspend fun fetchPaged(query: PageQuery): PageResult<UserItem>
    suspend fun delete(userId: String)
    suspend fun getById(userId: String): User
}
