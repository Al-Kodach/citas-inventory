package com.citacomsys.api.organization.model

import com.citacomsys.types.BrandingConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root request for establishing a new Company account.
 */
@Serializable
@SerialName("OrganizationRegistrationRequest")
data class OrganizationRegistrationRequest(
    val organizationName: String,
    val owner: OwnerRegistrationDetails,
    val brand: BrandingConfig? = null,
    val deviceId: String
)
