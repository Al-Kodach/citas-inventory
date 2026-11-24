package com.citacomsys.feature.auth.data.remote.api.ktor

import com.citacomsys.types.LoginRequest
import com.citacomsys.types.RemoteAuthResult
import com.citacomsys.api.auth.service.AuthApiService
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.ActRequest
import com.citacomsys.types.ActionToken
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.AuthenticatedSession
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.ResetPasswordSubmitRequest
import com.citacomsys.types.UserWorkspaceBinding
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class KtorAuthApi(
    client: HttpClient
) : BaseApi(client), AuthApiService {

    override suspend fun login(
        username: String,
        password: String
    ): RemoteAuthResult {
        return executeRequest {
            client.post("auth/login") {
                setBody(LoginRequest(identity = username, password = password))
            }.also { client.clearAuthTokens() }
        }
    }

    override suspend fun logout(accessToken: String) {
        executeRequest<Unit> {
            client.post("auth/logout") {
                headers.append("Authorization", "Bearer $accessToken")
            }.also { client.clearAuthTokens() }
        }
    }

    override suspend fun requestPasswordReset(identifier: String): AuthChallengeContext {
        return executeRequest {
            client.post("auth/password-resetWorkspaceSession/request") {
                setBody(mapOf("identifier" to identifier))
            }
        }
    }

    override suspend fun submitPasswordReset(challenge: AuthChallengeContext, otp: String, newPassword: String) {
        executeRequest<Unit> {
            client.post("auth/password-resetWorkspaceSession/submit") {
                setBody(ResetPasswordSubmitRequest(challenge = challenge, otp = otp, newPassword = newPassword))
            }
        }
    }

    override suspend fun generateAct(
        password: String,
        purpose: String,
        reason: String?
    ): ActionToken {
        return executeRequest {
            client.post("auth/act") {
                setBody(ActRequest(password = password, purpose = purpose, reason = reason))
            }
        }
    }

    override suspend fun resendOtp(phone: PhoneNumber) {
        executeRequest<Unit> {
            client.post("auth/resend-otp") {
                setBody(phone)
            }
        }
    }

    override suspend fun getActiveDevices(): Result<List<DeviceSummary>> = runCatching {
        executeRequest {
            client.get("auth/devices")
        }
    }

    override suspend fun fetchSelfWorkspaceBindings(): Result<List<UserWorkspaceBinding>> = runCatching {
        executeRequest {
            client.get("api/v1/users/self/workspace-bindings")
        }
    }

    override suspend fun verifyMfa(
        otp: String,
        authChallengeContext: AuthChallengeContext?
    ): AuthenticatedSession {
        return executeRequest {
            client.post("auth/verify-otp") {
                headers.append("X-OTP-Code", otp)
                authChallengeContext?.let {
                    setBody(authChallengeContext)
                }
            }.also { client.clearAuthTokens() }
        }
    }
}
