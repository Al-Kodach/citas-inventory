package com.citacomsys.kernel.service

import com.citacomsys.kernel.model.*
import com.citacomsys.types.WorkspaceClaimRequest

data class EtagFetchResult<T>(
    val data: T?,
    val etag: String?,
    val isNotModified: Boolean
)

interface KernelApiService {
    suspend fun getOrganizationConfig(organizationId: String? = null, etag: String? = null): EtagFetchResult<OrganizationRuntimeConfig>
    suspend fun getRuntimeState(workspaceId: String? = null): WorkspaceRuntimeState
    suspend fun getConfig(etag: String? = null): EtagFetchResult<WorkspaceRuntimeConfig>
    suspend fun refresh(workspaceId: String? = null, etag: String? = null): EtagFetchResult<Pair<WorkspaceRuntimeConfig, WorkspaceRuntimeState>>
    suspend fun getModulePolicy(currentVersion: Long?): ServerProductPolicy
    suspend fun claimWorkspace(request: WorkspaceClaimRequest): BootstrapWorkspaceResponse
}
