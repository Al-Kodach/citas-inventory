package com.citacomsys.admin.api.auth

import com.citacomsys.types.ActionToken
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.AuthenticatedSession
import com.citacomsys.types.RemoteAuthResult
import com.citacomsys.types.User

interface AdminAuthService {
    suspend fun login(identity: String, password: String): RemoteAuthResult
    suspend fun verifyMfa(identity: String, otpCode: String): Result<AuthenticatedSession>
    suspend fun generateActionToken(password: String, purpose: String, reason: String?): ActionToken
    suspend fun logout(): Result<Unit>
    suspend fun requestPasswordReset(identifier: String): Result<Unit>
    suspend fun submitPasswordReset(identifier: String, otp: String, newPassword: String): Result<Unit>
    suspend fun requestDeviceMigration(identifier: String, password: String): Result<Unit>
    suspend fun confirmDeviceMigration(identifier: String, otp: String, deviceId: String): Result<Unit>
}
