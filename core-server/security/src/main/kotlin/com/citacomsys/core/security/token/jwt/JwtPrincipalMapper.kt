package com.citacomsys.core.security.token.jwt

import com.auth0.jwt.interfaces.Payload
import com.citacomsys.types.ActionConfirmationPurpose

class JwtPrincipalMapper {

    fun from(payload: Payload): SecurityPrincipal {

        val type = TokenType.valueOf(
            payload.getClaim(JwtClaims.TYPE).asString()
        )

        return when (type) {

            TokenType.DEVICE -> {
                val scope = payload.getClaim(JwtClaims.SCOPE).asString()?.let {
                    AuthScope.valueOf(it)
                }!!
                val deviceId = payload.getClaim(JwtClaims.DEVICE_ID).asString()
                val organizationId = payload.getClaim(JwtClaims.ORGANIZATION_ID).asString()
                val workspaceId = payload.getClaim(JwtClaims.WORKSPACE_ID).asString()

                if (scope == AuthScope.ADMIN) {
                    AdminDevicePrincipal(hardwareId = deviceId)
                } else {
                    WorkstationDevicePrincipal(
                        hardwareId = deviceId,
                        organizationId = organizationId ?: error("Organization ID missing in device token"),
                        workspaceId = workspaceId ?: error("Workspace ID missing in device token")
                    )
                }
            }

            TokenType.ACCESS,
            TokenType.REFRESH -> {
                UserPrincipal(
                    tokenId = payload.getClaim(JwtClaims.TOKEN_ID).asString(),
                    userId = payload.getClaim(JwtClaims.USER_ID).asString(),
                    username = payload.getClaim(JwtClaims.USERNAME).asString(),
                    organizationId = payload.getClaim(JwtClaims.ORGANIZATION_ID).asString(),
                    workspaceId = payload.getClaim(JwtClaims.WORKSPACE_ID).asString(),
                    role = payload.getClaim(JwtClaims.ROLE).asString(),
                    sessionId = payload.getClaim(JwtClaims.SESSION_ID).asString(),
                    tokenType = type,
                    deviceId = payload.getClaim(JwtClaims.DEVICE_ID).asString()
                )
            }

            TokenType.ACTION -> {

                ActionPrincipal(
                    userId = payload.getClaim(JwtClaims.USER_ID).asString(),
                    organizationId = payload.getClaim(JwtClaims.ORGANIZATION_ID).asString(),
                    workspaceId = payload.getClaim(JwtClaims.WORKSPACE_ID).asString(),
                    role = payload.getClaim(JwtClaims.ROLE).asString(),
                    action = ActionConfirmationPurpose.valueOf(
                        payload.getClaim(JwtClaims.ACTION).asString()
                    ),
                    username = payload.getClaim(JwtClaims.USERNAME).asString(),
                    sessionId = payload.getClaim(JwtClaims.SESSION_ID).asString()
                )
            }
        }
    }
}
