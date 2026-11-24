package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokens (
     val accessToken: String,
     val refreshToken: String
)