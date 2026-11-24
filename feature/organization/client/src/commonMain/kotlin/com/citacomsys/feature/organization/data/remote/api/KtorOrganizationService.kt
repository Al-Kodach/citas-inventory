package com.citacomsys.feature.organization.data.remote.api

import com.citacomsys.api.organization.*
import com.citacomsys.api.organization.model.OrganizationRegistrationRequest
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.api.organization.model.UserSessionDto
import com.citacomsys.api.organization.model.VerificationResult
import com.citacomsys.api.organization.model.WorkstationBindingDto
import com.citacomsys.api.organization.model.WorkstationRecoveryRequest
import com.citacomsys.api.organization.model.WorkstationRecoveryResult
import com.citacomsys.api.workspace.model.WorkspaceDetails
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.BrandingConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.*

internal class KtorOrganizationService(client: HttpClient) : BaseApi(client),
    OrganizationService {
    private val endpoint = "api/v1/organization"

    override suspend fun registerOrganization(
        request: OrganizationRegistrationRequest
    ): OrganizationRegistrationResult =
        executeRequest {
            client.post("$endpoint/register") {
                setBody(request)
            }
        }

    override suspend fun verifyOwner(
        ownerId: String,
        code: String,
        fingerprint: String?,
        wsPublicKey: String?
    ): VerificationResult = executeRequest {
        client.post("$endpoint/verify") {
            setBody(
                mapOf(
                    "ownerId" to ownerId,
                    "code" to code
                )
            )
        }
    }

    override suspend fun recoverWorkstation(
        request: WorkstationRecoveryRequest,
        fingerprint: String?,
        wsPublicKey: String?
    ): WorkstationRecoveryResult = executeRequest {
        client.post("$endpoint/workstation/recover") {
            setBody(request)
        }
    }

    override suspend fun updateProfile(
        orgId: String,
        name: String,
        email: String?,
        phone: String?
    ): Result<Unit> = runCatching {
        executeRequest {
            client.put("$endpoint/identity") {
                setBody(
                    mapOf(
                        "id" to name,
                        "email" to email,
                        "phone" to phone
                    )
                )
            }
        }
    }

    override suspend fun deleteBrand(orgId: String): Unit = executeRequest {
        client.delete("$endpoint/branding")
    }

    override suspend fun saveBranding(
        orgId: String,
        brandingConfig: BrandingConfig
    ) {
        return executeRequest {
            client.post("$endpoint/branding") {
                setBody(brandingConfig)
            }
        }
    }

    override suspend fun getWorkspaceDetails(workspaceId: String): Result<WorkspaceDetails> = runCatching {
        executeRequest {
            client.get("$endpoint/workspace/$workspaceId/details")
        }
    }

    override suspend fun getWorkstationBindings(orgId: String): Result<List<WorkstationBindingDto>> = runCatching {
        executeRequest { client.get("$endpoint/workstation/bindings") }
    }

    override suspend fun revokeWorkstationBinding(bindingId: String): Result<Unit> = runCatching {
        executeRequest { client.post("$endpoint/workstation/bindings/$bindingId/revoke") }
    }

    override suspend fun getUserSessions(orgId: String): Result<List<UserSessionDto>> = runCatching {
        executeRequest { client.get("$endpoint/sessions") }
    }

    override suspend fun revokeUserSession(
        sessionId: String,
        reason: String?
    ): Result<Unit> = runCatching {
        executeRequest { client.post("$endpoint/sessions/$sessionId/revoke") { setBody(mapOf("reason" to reason)) } }
    }
}
