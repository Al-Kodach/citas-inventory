package com.citacomsys.feature.platform.data.remote.api.ktor

import com.citacomsys.api.platform.model.PlatformContent
import com.citacomsys.api.platform.remote.PlatformApiService
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get

internal class KtorPlatformApiService(
    client: HttpClient,
    private val endpoint: String = "api/v1/platform"
) : PlatformApiService, BaseApi(client) {

    override suspend fun getWelcomeContent(): Result<PlatformContent> = runCatching {
        executeRequest { client.get("$endpoint/welcome") }
    }
}
