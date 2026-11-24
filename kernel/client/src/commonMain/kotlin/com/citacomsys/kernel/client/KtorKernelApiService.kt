package com.citacomsys.kernel.client

import com.citacomsys.core.remote.ApiNetworkException
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.core.remote.HttpErrorMetadata
import com.citacomsys.core.remote.mapToApiException
import com.citacomsys.kernel.model.*
import com.citacomsys.kernel.service.EtagFetchResult
import com.citacomsys.kernel.service.KernelApiService
import com.citacomsys.types.WorkspaceClaimRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.io.IOException

internal class KtorKernelApiService(
    client: HttpClient
) : KernelApiService, BaseApi(client) {

    private val endpoint = "api/v1/kernel"

    override suspend fun getOrganizationConfig(
        organizationId: String?,
        etag: String?
    ): EtagFetchResult<OrganizationRuntimeConfig> {
        val response = try {
            client.get("$endpoint/organization/pkg") {
                etag?.let { header("If-None-Match", it) }
            }
        } catch (e: IOException) {
            throw ApiNetworkException(cause = e)
        }

        if (response.status == HttpStatusCode.NotModified) {
            return EtagFetchResult(data = null, etag = etag, isNotModified = true)
        }

        if (response.status.isSuccess()) {
            val data: OrganizationRuntimeConfig = response.body()
            val newEtag = response.headers["ETag"]
            return EtagFetchResult(data = data, etag = newEtag, isNotModified = false)
        }

        val bodyText = safeBodyAsText(response)
        val errorInfo = HttpErrorMetadata(
            method = response.request.method,
            url = response.request.url,
            status = response.status,
            requestId = response.headers["X-Request-Id"]
        )
        throw mapToApiException(bodyText, response, errorInfo)
    }

    override suspend fun getRuntimeState(workspaceId: String?): WorkspaceRuntimeState {
        return executeRequest {
            client.get("$endpoint/workspace/state") {
                workspaceId?.let {
                    url {
                        headers.remove("X-Workspace-Id")
                        headers.append("X-Workspace-Id", workspaceId)
                    }
                }
            }
        }
    }

    override suspend fun getConfig(etag: String?): EtagFetchResult<WorkspaceRuntimeConfig> {
        val response = try {
            client.get("$endpoint/workspace/pkg") {
                etag?.let { header("If-None-Match", it) }
            }
        } catch (e: IOException) {
            throw ApiNetworkException(cause = e)
        }

        if (response.status == HttpStatusCode.NotModified) {
            return EtagFetchResult(data = null, etag = etag, isNotModified = true)
        }

        if (response.status.isSuccess()) {
            val data: WorkspaceRuntimeConfig = response.body()
            val newEtag = response.headers["ETag"]
            return EtagFetchResult(data = data, etag = newEtag, isNotModified = false)
        }

        val bodyText = safeBodyAsText(response)
        val errorInfo = HttpErrorMetadata(
            method = response.request.method,
            url = response.request.url,
            status = response.status,
            requestId = response.headers["X-Request-Id"]
        )
        throw mapToApiException(bodyText, response, errorInfo)
    }

    override suspend fun refresh(
        workspaceId: String?,
        etag: String?
    ): EtagFetchResult<Pair<WorkspaceRuntimeConfig, WorkspaceRuntimeState>> {
        val response = try {
            client.get("$endpoint/workspace/profile/refresh") {
                workspaceId?.let {
                    url {
                        headers.remove("X-Workspace-Id")
                        headers.append("X-Workspace-Id", workspaceId)
                    }
                }
                etag?.let { header("If-None-Match", it) }
            }
        } catch (e: IOException) {
            throw ApiNetworkException(cause = e)
        }

        if (response.status == HttpStatusCode.NotModified) {
            return EtagFetchResult(data = null, etag = etag, isNotModified = true)
        }

        if (response.status.isSuccess()) {
            val data: Pair<WorkspaceRuntimeConfig, WorkspaceRuntimeState> = response.body()
            val newEtag = response.headers["ETag"]
            return EtagFetchResult(data = data, etag = newEtag, isNotModified = false)
        }

        val bodyText = safeBodyAsText(response)
        val errorInfo = HttpErrorMetadata(
            method = response.request.method,
            url = response.request.url,
            status = response.status,
            requestId = response.headers["X-Request-Id"]
        )
        throw mapToApiException(bodyText, response, errorInfo)
    }

    override suspend fun getModulePolicy(currentVersion: Long?): ServerProductPolicy {
        return executeRequest {
            client.get("$endpoint/workspace/modules/policy") {
                url {
                    currentVersion?.let { parameters.append("version", it.toString()) }
                }
            }
        }
    }

    override suspend fun claimWorkspace(
        request: WorkspaceClaimRequest
    ): BootstrapWorkspaceResponse {
        return executeRequest {
            client.post("$endpoint/workspace/claim") {
                setBody(request)
            }
        }
    }
}
