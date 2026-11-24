package com.citacomsys.inventory.admin.tenant.remote

import com.citacomsys.admin.api.model.WorkspaceDetails
import com.citacomsys.admin.api.tenant.AdminPreRegisterRequest
import com.citacomsys.admin.api.tenant.OrganizationDetails
import com.citacomsys.admin.api.tenant.TenantApiService
import com.citacomsys.api.organization.model.Organization
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class TenantApiServiceImpl(
    client: HttpClient,
    private val endpoint: String = "api/v1/admin/tenants"
) : TenantApiService, BaseApi(client) {

    override suspend fun fetchOrganizations(query: PageQuery): PageResult<Organization> {
        return executeRequest {
            client.post(endpoint) {
                setBody(query)
            }
        }
    }

    override suspend fun getOrganizationDetails(orgId: String): OrganizationDetails {
        return executeRequest { client.get("$endpoint/$orgId") }
    }

    override suspend fun suspendOrganization(orgId: String, reason: String): Result<Unit> =
        runCatching {
            executeRequest {
                client.post("$endpoint/$orgId/suspend") {
                    parameter("reason", reason)
                }
            }
        }

    override suspend fun activateOrganization(orgId: String): Result<Unit> = runCatching {
        executeRequest { client.post("$endpoint/$orgId/activateServices") }
    }

    override suspend fun preRegisterOrganization(request: AdminPreRegisterRequest): OrganizationRegistrationResult {
        return executeRequest {
            client.post("$endpoint/pre-register") {
                setBody(request)
            }
        }
    }

    override suspend fun getWorkspaceDetails(orgId: String, id: String): WorkspaceDetails {
        return executeRequest { client.get("$endpoint/$orgId/workspaces/$id") }
    }

    override suspend fun suspendWorkspace(
        orgId: String,
        id: String,
        reason: String
    ): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/$orgId/workspaces/$id/suspend") {
                parameter("reason", reason)
            }
        }
    }

    override suspend fun activateWorkspace(orgId: String, id: String): Result<Unit> = runCatching {
        executeRequest { client.post("$endpoint/$orgId/workspaces/$id/activateServices") }
    }

    override suspend fun deleteWorkspace(orgId: String, id: String): Result<Unit> = runCatching {
        executeRequest { client.delete("$endpoint/$orgId/workspaces/$id") }
    }
}
