package com.citacomsys.inventory.admin.auth.remote.api

import com.citacomsys.admin.api.auth.AdminAuthService
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.ActRequest
import com.citacomsys.types.ActionToken
import com.citacomsys.types.AuthenticatedSession
import com.citacomsys.types.LoginRequest
import com.citacomsys.types.RemoteAuthResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class KtorAdminAuthApiService(
    client: HttpClient,
    private val endpoint: String = "/admin/auth"
) : AdminAuthService, BaseApi(client) {

    override suspend fun verifyMfa(identity: String, otpCode: String): Result<AuthenticatedSession> = runCatching {
        executeRequest {
            client.post("$endpoint/verify-mfa") {
                header("X-OTP-Code", otpCode)
                header("X-User-Id", identity)
            }
        }
    }

    override suspend fun login(
        identity: String,
        password: String
    ): RemoteAuthResult = executeRequest {
        val request = LoginRequest(
            identity = identity,
            password = password
        )
        client.post("$endpoint/login") {
            setBody(request)
        }
    }

    override suspend fun generateActionToken(
        password: String,
        purpose: String,
        reason: String?
    ): ActionToken = executeRequest {
        client.post("$endpoint/act") {
            setBody(
                ActRequest(password, purpose, reason)
            )
        }
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/logout")
        }
    }

    override suspend fun requestPasswordReset(identifier: String): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/password/resetWorkspaceSession") {
                setBody(mapOf("identifier" to identifier))
            }
        }
    }

    override suspend fun submitPasswordReset(
        identifier: String,
        otp: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/password/submit") {
                setBody(mapOf("identifier" to identifier, "otp" to otp, "newPassword" to newPassword))
            }
        }
    }

    override suspend fun requestDeviceMigration(identifier: String, password: String): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/device/migration/request") {
                setBody(mapOf("identifier" to identifier, "password" to password))
            }
        }
    }

    override suspend fun confirmDeviceMigration(identifier: String, otp: String, deviceId: String): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/device/migration/confirm") {
                setBody(mapOf("identifier" to identifier, "otp" to otp, "deviceId" to deviceId))
            }
        }
    }
}
