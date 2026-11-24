package com.citacomsys.core.domain.security

import com.citacomsys.types.ActionToken
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.AuthenticatedSession
import com.citacomsys.types.RemoteAuthResult
import com.citacomsys.types.User

interface AuthService {

    suspend fun login(identity: String, password: String): RemoteAuthResult
    suspend fun generateActionToken(
        password: String,
        purpose: String,
        reason: String?
    ): ActionToken


    suspend fun logout()

    suspend fun requestPasswordReset(identifier: String): Result<AuthChallengeContext>

    suspend fun submitPasswordReset(
        challenge: AuthChallengeContext,
        otp: String,
        newPassword: String
    ): Result<Unit>

    suspend fun enrollAuthUser(
        tokens: AuthTokens,
        user: User,
        password: String
    )

    suspend fun verifyMfa(otpCode: String): Result<AuthenticatedSession>
}
