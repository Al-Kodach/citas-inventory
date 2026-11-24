package com.citacomsys.feature.workspace.data.remote

import com.citacomsys.api.workspace.branch.Branch
import com.citacomsys.api.workspace.branch.BranchRepository
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class KtorBranchRepository(
    client: HttpClient
) : BranchRepository, BaseApi(client) {
    private val endpoint = "api/v1/workspaces"

    override suspend fun save(branch: Branch) {
        return executeRequest {
            client.post("$endpoint/${branch.workspaceId}/branches") {
                setBody(branch)
            }
        }
    }

    override suspend fun get(workspaceId: String, id: String): Branch {
        return executeRequest<Branch> {
            client.get("$endpoint/$workspaceId/branches/$id")
        }
    }

    override suspend fun getAll(workspaceId: String): List<Branch> {
        return executeRequest {
            client.get("$endpoint/$workspaceId/branches")
        }
    }

    override suspend fun delete(workspaceId: String, id: String) {
        return executeRequest<Unit> {
            client.delete("$endpoint/$workspaceId/branches/$id")
        }
    }
}