package com.citacomsys.feature.workspace.data.remote.api

import com.citacomsys.api.workspace.model.SubscriptionSummary
import com.citacomsys.api.workspace.model.WorkspaceSubscription
import com.citacomsys.api.workspace.service.WorkspaceApiService
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.BrandingConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.*

internal class KtorWorkspaceApiService(
    client: HttpClient,
    private val endpoint: String = "api/v1/workspace"
) : WorkspaceApiService, BaseApi(client) {

    override suspend fun getSubscription(): Result<WorkspaceSubscription> {
        return runCatching {
            executeRequest { client.get("$endpoint/subscription") }
        }
    }

    override suspend fun getSubscriptionSummary(): SubscriptionSummary {
        return executeRequest { client.get("$endpoint/subscription/summary") }
    }

    override suspend fun updateProfile(
        name: String,
        email: String?,
        phone: String?
    ): Result<Unit> = runCatching {
        executeRequest {
            client.put("$endpoint/identity") {
                setBody(
                    mapOf(
                        "id" to name,
                        "email" to email,
                        "phone" to phone
                    )
                )
            }
        }
    }

    override suspend fun updateBranding(config: BrandingConfig?): Result<Unit> = runCatching {
        executeRequest {
            if (config == null) {
                client.delete("$endpoint/branding")
            } else {
                client.post("$endpoint/branding") {
                    setBody(config)
                }
            }
        }
    }
}
