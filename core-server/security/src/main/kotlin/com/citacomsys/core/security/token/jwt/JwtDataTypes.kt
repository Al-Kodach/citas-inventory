package com.citacomsys.core.security.token.jwt

import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.types.ActionConfirmationPurpose

enum class AuthScope {
    USER,
    ADMIN
}

enum class TokenType {
    DEVICE,
    ACCESS,
    REFRESH,
    ACTION
}

object JwtClaims {
    const val TYPE = "typ"
    const val SCOPE = "sup"

    const val DEVICE_ID = "did"
    const val WORKSPACE_ID = "tid"
    const val ORGANIZATION_ID = "oid"

    const val USER_ID = "uid"
    const val USERNAME = "uname"
    const val ROLE = "role"

    const val SESSION_ID = "sid"

    const val ACTION = "act"

    const val TOKEN_ID = "jti"
}


sealed interface SecurityPrincipal {
    val tokenType: TokenType
}

sealed class DevicePrincipal : SecurityPrincipal {
    abstract val hardwareId: String
    abstract val organizationId: String?
    abstract val workspaceId: String? 
    override val tokenType: TokenType = TokenType.DEVICE
}

data class WorkstationDevicePrincipal(
    override val hardwareId: String,
    override val organizationId: String?,
    override val workspaceId: String?,
) : DevicePrincipal()


data class AdminDevicePrincipal(
    override val hardwareId: String,
    override val organizationId: String = SystemActors.SYSTEM_ID,
    override val workspaceId: String = SystemActors.SYSTEM_ID
) : DevicePrincipal()

data class UserPrincipal(
    val tokenId: String?,
    val userId: String,
    val username: String,
    val organizationId: String,
    val workspaceId: String?, 
    val deviceId: String,
    val role: String,
    val sessionId: String,
    override val tokenType: TokenType,
) : SecurityPrincipal

data class ActionPrincipal(
    val userId: String,
    val organizationId: String,
    val workspaceId: String,
    val username: String,
    val role: String,
    val action: ActionConfirmationPurpose,
    val sessionId: String,
) : SecurityPrincipal {
    override val tokenType: TokenType = TokenType.ACTION
}

data class TokenConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val durationMillis: Long
)
