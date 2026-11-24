package com.citacomsys.core.security.service

import com.citacomsys.core.security.token.GeneratedRefreshToken
import com.citacomsys.core.security.token.jwt.JWTTokenProvider
import com.citacomsys.core.security.token.jwt.JwtClaims
import com.citacomsys.core.security.token.jwt.TokenType
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

data class AccessTokenPair(
    val accessToken: String,
    val refreshToken: GeneratedRefreshToken,
)

class UserTokenIssuer(
    private val accessProvider: JWTTokenProvider,
    private val refreshProvider: JWTTokenProvider,
) {
    fun issueAccess(
        userId: String,
        organizationId: String,
        workspaceId: String?,
        username: String,
        deviceId: String,
        role: String,
        sessionId: String,
    ): String {

        return accessProvider.generate(
            buildMap {
                put(JwtClaims.TYPE, TokenType.ACCESS.name)
                put(JwtClaims.USERNAME, username)
                put(JwtClaims.USER_ID, userId)
                put(JwtClaims.ORGANIZATION_ID, organizationId)
                if (workspaceId != null) put(JwtClaims.WORKSPACE_ID, workspaceId)
                put(JwtClaims.DEVICE_ID, deviceId)
                put(JwtClaims.ROLE, role)
                put(JwtClaims.SESSION_ID, sessionId)
            }
        )
    }

    fun issueRefresh(
        userId: String,
        organizationId: String,
        workspaceId: String?,
        role: String,
        sessionId: String,
        deviceId: String,
        username: String
    ): GeneratedRefreshToken {
        val tokenId = UUID.randomUUID()
        val expiresAt = Clock.System.now() + refreshProvider.config.durationMillis.milliseconds

        val token = refreshProvider.generate(
            buildMap {
                put(JwtClaims.TYPE, TokenType.REFRESH.name)
                put(JwtClaims.DEVICE_ID, deviceId)
                put(JwtClaims.USERNAME, username)
                put(JwtClaims.USER_ID, userId)
                put(JwtClaims.ORGANIZATION_ID, organizationId)
                if (workspaceId != null) put(JwtClaims.WORKSPACE_ID, workspaceId)
                put(JwtClaims.ROLE, role)
                put(JwtClaims.SESSION_ID, sessionId)
                put(JwtClaims.TOKEN_ID, tokenId.toString())
            }
        )

        return GeneratedRefreshToken(token, tokenId, expiresAt)
    }

    fun issuePair(
        userId: String,
        organizationId: String,
        workspaceId: String?,
        role: String,
        username: String,
        sessionId: String,
        deviceId: String
    ): AccessTokenPair {

        return AccessTokenPair(
            accessToken = issueAccess(
                userId = userId,
                organizationId = organizationId,
                workspaceId = workspaceId,
                username = username,
                deviceId = deviceId,
                role = role,
                sessionId = sessionId
            ),
            refreshToken = issueRefresh(
                userId = userId,
                organizationId = organizationId,
                workspaceId = workspaceId,
                role = role,
                username = username,
                deviceId = deviceId,
                sessionId = sessionId
            )
        )
    }
}
