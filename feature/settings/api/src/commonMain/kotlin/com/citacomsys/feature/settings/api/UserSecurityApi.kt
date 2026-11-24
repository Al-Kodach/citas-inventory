package com.citacomsys.feature.settings.api

import com.citacomsys.types.AuthChallengeContext

interface UserSecurityApi {
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    suspend fun requestMfaEnable(): Result<AuthChallengeContext>
    suspend fun confirmMfaEnable(otp: String): Result<Unit>
    suspend fun requestMfaDisableAct(): Result<AuthChallengeContext>
    suspend fun disableMfa(otp: String): Result<Unit>
}
