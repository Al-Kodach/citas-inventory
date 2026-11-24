package com.citacomsys.core.security.service

import com.citacomsys.core.security.token.jwt.AuthScope
import com.citacomsys.core.security.token.jwt.JWTTokenProvider
import com.citacomsys.core.security.token.jwt.JwtClaims
import com.citacomsys.core.security.token.jwt.TokenType

class DeviceTokenIssuer(
    private val provider: JWTTokenProvider
) {

    fun issue(
        deviceId: String,
        organizationId: String,
        workspaceId: String,
        scope: AuthScope
    ): String {
        return provider.generate(
            mapOf(
                JwtClaims.TYPE to TokenType.DEVICE.name,
                JwtClaims.DEVICE_ID to deviceId,
                JwtClaims.ORGANIZATION_ID to organizationId,
                JwtClaims.WORKSPACE_ID to workspaceId,
                JwtClaims.SCOPE to scope.name
            )
        )
    }
}
