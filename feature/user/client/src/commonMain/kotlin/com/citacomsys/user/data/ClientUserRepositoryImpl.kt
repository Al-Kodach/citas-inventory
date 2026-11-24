package com.citacomsys.user.data

import com.citacomsys.core.database.dao.AppUserDao
import com.citacomsys.core.security.OfflineAuthenticator
import com.citacomsys.inventory.api.user.model.BindUserToWorkspaceRequest
import com.citacomsys.inventory.api.user.model.SaveTenantUserRequest
import com.citacomsys.inventory.api.user.model.SuspendUserRequest
import com.citacomsys.inventory.api.user.model.UnbindUserFromWorkspaceRequest
import com.citacomsys.shared.util.resultOf
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.User
import com.citacomsys.types.UserItem
import com.citacomsys.types.UserWorkspaceBinding
import com.citacomsys.user.data.remote.api.UserApiService

internal class ClientUserRepositoryImpl(
    private val api: UserApiService,
    private val dao: AppUserDao,
    private val authenticator: OfflineAuthenticator
) : ClientUserRepository {

    override suspend fun save(request: SaveTenantUserRequest) {
        api.save(request)
    }

    override suspend fun bindUserToWorkspace(request: BindUserToWorkspaceRequest): Result<Unit> {
        return resultOf {
            api.bindUserToWorkspace(request)
        }
    }

    override suspend fun unbindUserFromWorkspace(request: UnbindUserFromWorkspaceRequest): Result<Unit> {
        return resultOf {
            api.unbindUserFromWorkspace(request)
        }
    }

    override suspend fun findByIdRemote(id: String): Result<User> {
        return resultOf { api.getById(id) }
    }

    override suspend fun suspendUser(
        request: SuspendUserRequest
    ): Result<Unit> {
        return resultOf {
            api.suspend(request.userId, request)
            dao.suspend(request.userId, request.reason)
        }
    }

    override suspend fun activate(userId: String): Result<Unit> {
        return resultOf {
            api.activate(userId)
            dao.activate(userId)
        }
    }

    override suspend fun delete(userId: String): Result<Unit> {
        return resultOf {
            api.delete(userId)
            dao.delete(userId)
            authenticator.revokeUsers(setOf(userId))
        }
    }

    override suspend fun getWorkspaceBindingsForUser(userId: String): Result<List<UserWorkspaceBinding>> {
        return resultOf { api.getUserWorkspaceBindings(userId) }
    }

    override suspend fun syncCloudPage(query: PageQuery): PageResult<UserItem> {
        return api.fetchPaged(query)
    }
}
