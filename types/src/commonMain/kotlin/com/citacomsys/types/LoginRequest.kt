package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("LoginRequest")
data class LoginRequest(
    val identity: String,
    val password: String
)

@Serializable
@SerialName("ActRequest")
data class ActRequest(
    val password: String,
    val purpose: String,
    val reason: String? = null
)

@Serializable
@SerialName("ActionToken")
data class ActionToken(
    val token: String
)