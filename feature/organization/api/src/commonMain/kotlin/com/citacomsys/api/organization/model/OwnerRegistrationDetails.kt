package com.citacomsys.api.organization.model

import com.citacomsys.types.PhoneNumber
import kotlinx.serialization.Serializable

@Serializable
data class OwnerRegistrationDetails(
    val name: String,
    val username: String,
    val email: String,
    val phone: PhoneNumber,
    val password: String,
    val mfaEnabled: Boolean = true
)
