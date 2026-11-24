package com.citacomsys.user.data.remote.api.impl

import com.citacomsys.core.remote.BaseApi
import com.citacomsys.inventory.api.user.model.*
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.User
import com.citacomsys.types.UserItem
import com.citacomsys.types.UserWorkspaceBinding
import com.citacomsys.user.data.remote.api.UserApiService
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

internal class KtorUserApiService(
    client: HttpClient,
    private val endpoint: String = "api/v1/users"
) : BaseApi(client), UserApiService {

    override suspend fun getById(userId: String): User {
        return executeRequest { client.get("$endpoint/$userId") }
    }

    override suspend fun delete(userId: String) {
        return executeRequest { client.delete("$endpoint/$userId") }
    }

    override suspend fun save(request: SaveTenantUserRequest) {
        return executeRequest {
            client.post(endpoint) {
                setBody(request)
            }
        }
    }

    override suspend fun bindUserToWorkspace(request: BindUserToWorkspaceRequest) {
        return executeRequest {
            client.post("$endpoint/bind-workspace") {
                setBody(request)
            }
        }
    }

    override suspend fun unbindUserFromWorkspace(request: UnbindUserFromWorkspaceRequest) {
        return executeRequest {
            client.post("$endpoint/unbind-workspace") {
                setBody(request)
            }
        }
    }

    override suspend fun suspend(userId: String, request: SuspendUserRequest) {
        return executeRequest {
            client.put("$endpoint/$userId/suspend") {
                setBody(request)
            }
        }
    }

    override suspend fun activate(userId: String) {
        return executeRequest {
            client.put("$endpoint/$userId/activateServices")
        }
    }

    override suspend fun updatePassword(id: String, request: UpdatePasswordRequest) {
        return executeRequest {
            client.put("$endpoint/$id/change-pwd") {
                setBody(request)
            }
        }
    }

    override suspend fun getUserWorkspaceBindings(userId: String): List<UserWorkspaceBinding> {
        return executeRequest { client.get("$endpoint/$userId/workspace-bindings") }
    }

    override suspend fun fetchPaged(query: PageQuery): PageResult<UserItem> {
        return executeRequest {
            client.post("$endpoint/paged") {
                setBody(query)
            }
        }
    }
}
