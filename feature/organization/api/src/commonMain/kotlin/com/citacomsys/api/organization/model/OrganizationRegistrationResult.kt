package com.citacomsys.api.organization.model

import com.citacomsys.kernel.model.OrganizationRuntimeConfig
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.User
import kotlinx.serialization.Serializable

@Serializable
data class OrganizationRegistrationResult(
    val organizationId: String,
    val ownerId: String,
    val ownerPhoneMasked: String
)

@Serializable
data class VerificationResult(
    val config: OrganizationRuntimeConfig,
    val tokens: AuthTokens,
    val user: User
)
