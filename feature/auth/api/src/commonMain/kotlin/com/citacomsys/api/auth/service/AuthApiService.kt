package com.citacomsys.api.auth.service

import com.citacomsys.types.ActionToken
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.AuthenticatedSession
import com.citacomsys.types.RemoteAuthResult
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.UserWorkspaceBinding

interface AuthApiService {
    suspend fun login(username: String, password: String): RemoteAuthResult

    suspend fun logout(accessToken: String)
    suspend fun submitPasswordReset(
        challenge: AuthChallengeContext,
        otp: String,
        newPassword: String
    )

    suspend fun requestPasswordReset(identifier: String): AuthChallengeContext

    suspend fun generateAct(
        password: String,
        purpose: String,
        reason: String?,
    ): ActionToken

    suspend fun resendOtp(phone: PhoneNumber)
    suspend fun getActiveDevices(): Result<List<DeviceSummary>>
    suspend fun fetchSelfWorkspaceBindings(): Result<List<UserWorkspaceBinding>>
    suspend fun verifyMfa(
        otp: String,
        authChallengeContext: AuthChallengeContext?
    ): AuthenticatedSession
}
