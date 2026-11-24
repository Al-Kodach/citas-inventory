package com.citacomsys.auth.session

import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.types.AuthTokens

interface RefreshTokenService {

    suspend fun refresh(
        principal: UserPrincipal,
        rawRefreshToken: String,
        requestIp: String
    ): AuthTokens
}