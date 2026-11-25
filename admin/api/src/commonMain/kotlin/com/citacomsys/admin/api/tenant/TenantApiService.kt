package com.citacomsys.admin.api.tenant

import com.citacomsys.admin.api.model.WorkspaceDetails
import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.api.organization.model.Organization
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.api.workspace.model.Workspace
import com.citacomsys.types.Address
import com.citacomsys.types.Money
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.WorkspaceStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class BranchSummary(
    val id: String,
    val name: String,
    val locationCode: String,
    val email: String? = null,
    val phone: String? = null,
    val address: Address? = null,
    val isActive: Boolean
)

@Serializable
data class AdminWorkspaceSummary(
    val id: String,
    val name: String,
    val businessType: String = "",
    val status: WorkspaceStatus,
    val recurringAmount: Money?,
    val branches: List<BranchSummary>,
    val createdAt: Instant
)

@Serializable
@SerialName("OrganizationDetails")
data class OrganizationDetails(
    val id: String,
    val name: String,
    val businessCode: String,
    val email: String?,
    val phone: String?,
    val status: OrgStatus,
    val createdAt: Instant,
    val totalRecurringAmount: Money,
    val workspaces: List<AdminWorkspaceSummary>
)

@Serializable
data class AdminPreRegisterRequest(
    val organizationName: String,
    val ownerName: String = "Owner",
    val ownerPhone: PhoneNumber,
    val ownerEmail: String?,
    val username: String
)

@Serializable
data class PublicTenantResolveResult(
    val found: Boolean,
    val organizationName: String? = null,
    val businessCode: String? = null,
    val status: String? = null,
    val serverEnvironment: String = "staging",
    val recommendedServerUrl: String = "https://api.citacomsys.com/staging/"
)

interface TenantApiService {
    suspend fun fetchOrganizations(query: PageQuery): PageResult<Organization>
    suspend fun getOrganizationDetails(orgId: String): OrganizationDetails
    suspend fun suspendOrganization(orgId: String, reason: String): Result<Unit>
    suspend fun activateOrganization(orgId: String): Result<Unit>
    suspend fun preRegisterOrganization(request: AdminPreRegisterRequest): OrganizationRegistrationResult

    suspend fun getWorkspaceDetails(orgId: String, id: String): WorkspaceDetails
    suspend fun suspendWorkspace(orgId: String, id: String, reason: String): Result<Unit>
    suspend fun activateWorkspace(orgId: String, id: String): Result<Unit>
    suspend fun deleteWorkspace(orgId: String, id: String): Result<Unit>
}
