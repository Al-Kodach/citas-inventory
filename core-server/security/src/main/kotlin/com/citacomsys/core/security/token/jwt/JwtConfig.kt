package com.citacomsys.core.security.token.jwt

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val userAccessSecret: String,
    val userRefreshSecret: String,
    val deviceSecret: String,
    val accessTokenDurationMinutes: Long,
    val refreshTokenDurationDays: Long,
    val deviceTokenDurationDays: Long
)