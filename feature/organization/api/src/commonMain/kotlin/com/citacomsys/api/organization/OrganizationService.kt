package com.citacomsys.api.organization

import com.citacomsys.api.organization.model.OrganizationRegistrationRequest
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.api.organization.model.UserSessionDto
import com.citacomsys.api.organization.model.VerificationResult
import com.citacomsys.api.organization.model.WorkstationBindingDto
import com.citacomsys.api.organization.model.WorkstationRecoveryRequest
import com.citacomsys.api.organization.model.WorkstationRecoveryResult
import com.citacomsys.api.workspace.model.WorkspaceDetails
import com.citacomsys.types.BrandingConfig

interface OrganizationService {
    /**
     * Entry point for a full company sign-up.
     */
    suspend fun registerOrganization(
        request: OrganizationRegistrationRequest
    ): OrganizationRegistrationResult

    /**
     * Verifies the owner of a newly registered organization.
     */
    suspend fun verifyOwner(
        ownerId: String,
        code: String,
        fingerprint: String? = null,
        wsPublicKey: String? = null
    ): VerificationResult

    /**
     * Recovers a locked or mismatched workstation/hub node.
     */
    suspend fun recoverWorkstation(
        request: WorkstationRecoveryRequest,
        fingerprint: String? = null,
        wsPublicKey: String? = null
    ): WorkstationRecoveryResult

    /**
     * Updates the legal company profile.
     */
    suspend fun updateProfile(
        orgId: String = "",
        name: String,
        email: String?,
        phone: String?
    ): Result<Unit>

    suspend fun deleteBrand(orgId: String)
    suspend fun saveBranding(orgId: String, brandingConfig: BrandingConfig)

    /**
     * Queries detailed information (subscription, resources, account, usage) for a specific workspace.
     */
    suspend fun getWorkspaceDetails(workspaceId: String): Result<WorkspaceDetails>

    suspend fun getWorkstationBindings(orgId: String): Result<List<WorkstationBindingDto>>
    suspend fun revokeWorkstationBinding(bindingId: String): Result<Unit>
    suspend fun getUserSessions(orgId: String): Result<List<UserSessionDto>>
    suspend fun revokeUserSession(sessionId: String, reason: String?): Result<Unit>
}
