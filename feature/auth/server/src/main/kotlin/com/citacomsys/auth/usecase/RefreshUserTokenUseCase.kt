package com.citacomsys.auth.usecase

import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.types.AuthTokens
import com.citacomsys.auth.session.RefreshTokenService

class RefreshUserTokenUseCase(
    private val service: RefreshTokenService
) {
    suspend fun execute(
        principal: UserPrincipal,
        rawRefreshToken: String,
        requestIp: String
    ): AuthTokens {
        return service.refresh(
            principal = principal,
            rawRefreshToken = rawRefreshToken,
            requestIp = requestIp
        )
    }
}