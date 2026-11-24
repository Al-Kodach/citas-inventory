package com.citacomsys.feature.platform.data.remote.api.ktor

import com.citacomsys.api.platform.model.SmsPackage
import com.citacomsys.api.platform.remote.SmsPackageService
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class KtorSmsPackageService(
    client: HttpClient,
    private val endpoint: String = "api/v1/sms-packages"
) : SmsPackageService, BaseApi(client) {
    override suspend fun get(id: String): SmsPackage {
        return executeRequest { client.get("$endpoint/$id") }
    }


    override suspend fun getActive(): List<SmsPackage> {
        return executeRequest { client.get(endpoint) }
    }

    override suspend fun save(pkg: SmsPackage) {
        return executeRequest {
            client.post(endpoint) {
                setBody(pkg)
            }
        }
    }

    override suspend fun delete(id: String) {
        return executeRequest { client.delete("$endpoint/$id") }
    }
}