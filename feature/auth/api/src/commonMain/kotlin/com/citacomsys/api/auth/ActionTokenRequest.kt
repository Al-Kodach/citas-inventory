package com.citacomsys.api.auth

import com.citacomsys.types.PhoneNumber
import kotlinx.serialization.Serializable

@Serializable
data class ActionTokenRequest(
    val phoneNumber: PhoneNumber,
    val password: String,
    val reason: String,
)

