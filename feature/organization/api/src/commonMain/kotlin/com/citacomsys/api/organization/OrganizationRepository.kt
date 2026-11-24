package com.citacomsys.api.organization

import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.api.organization.model.Organization
import com.citacomsys.api.organization.model.UserSessionDto
import com.citacomsys.api.organization.model.WorkstationBindingDto
import com.citacomsys.api.workspace.model.WorkspaceDetails
import com.citacomsys.kernel.model.OrganizationRuntimeConfig
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.HardwareMasterStatus

interface OrganizationRepository {
    suspend fun get(id: String): Result<Organization>
    suspend fun existsByBusinessCode(code: String): Boolean
    suspend fun existsByName(name: String): Boolean
    suspend fun findByBusinessCode(code: String): Result<Organization>
    suspend fun create(
        name: String,
        email: String?,
        phone: String?,
        businessCode: String,
        fleetDna: String,
        brandingConfig: BrandingConfig?
    ): Result<Organization>

    suspend fun updateOwnerId(organizationId: String, ownerId: String)
    suspend fun updateProfile(
        organizationId: String,
        name: String,
        email: String?,
        phone: String?
    )

    // Aggregation Methods
    suspend fun getRuntimeConfig(organizationId: String): OrganizationRuntimeConfig
    suspend fun updateStatus(orgId: String, status: OrgStatus)
    suspend fun getWorkspaceDetails(workspaceId: String, organizationId: String): WorkspaceDetails

    // Device & Session Management
    suspend fun getWorkstationBindings(organizationId: String): List<WorkstationBindingDto>
    suspend fun revokeWorkstationBinding(bindingId: String)
}
