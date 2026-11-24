package com.citacomsys.core.security.service

import com.citacomsys.core.security.token.jwt.JWTTokenProvider
import com.citacomsys.core.security.token.jwt.JwtClaims
import com.citacomsys.core.security.token.jwt.TokenType
import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.types.ActionConfirmationPurpose

class ActionTokenIssuer(
    private val provider: JWTTokenProvider
) {

    fun issue(
        userId: String,
        organizationId: String,
        workspaceId: String?,
        username: String,
        sessionId: String,
        role: String,
        purpose: ActionConfirmationPurpose
    ): String {

        return provider.generate(
            claims = mapOf(
                JwtClaims.TYPE to TokenType.ACTION.name,
                JwtClaims.USER_ID to userId,
                JwtClaims.ORGANIZATION_ID to organizationId,
                JwtClaims.WORKSPACE_ID to (workspaceId ?: ""),
                JwtClaims.USERNAME to username,
                JwtClaims.ROLE to role,
                JwtClaims.SESSION_ID to sessionId,
                JwtClaims.ACTION to purpose.name,
            ),
            durationMillis = 300_000L // 5 minutes
        )
    }
}
