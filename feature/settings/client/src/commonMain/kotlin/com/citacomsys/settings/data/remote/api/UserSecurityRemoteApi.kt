package com.citacomsys.settings.data.remote.api

import com.citacomsys.feature.settings.api.UserSecurityApi
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.AuthChallengeContext
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class KtorUserSecurityApi(
    client: HttpClient,
    private val endpoint: String = "api/v1/settings/profile"
) : UserSecurityApi, BaseApi(client) {

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/change-password") {
                setBody(mapOf("currentPassword" to currentPassword, "newPassword" to newPassword))
            }
        }
    }

    override suspend fun requestMfaEnable(): Result<AuthChallengeContext> = runCatching {
        executeRequest<AuthChallengeContext> {
            client.post("$endpoint/mfa/enable/request")
        }
    }

    override suspend fun confirmMfaEnable(otp: String): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/mfa/enable/confirm") {
                setBody(mapOf("otp" to otp))
            }
        }
    }

    override suspend fun requestMfaDisableAct(): Result<AuthChallengeContext> = runCatching {
        executeRequest<AuthChallengeContext> {
            client.post("$endpoint/mfa/disable/request")
        }
    }

    override suspend fun disableMfa(otp: String): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/mfa/disable") {
                setBody(mapOf("otp" to otp))
            }
        }
    }
}
