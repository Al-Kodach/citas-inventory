package com.citacomsys.retail.shared.data.remote

import com.citacomsys.module.inventory.data.remote.api.ktor.KtorProductApiService
import com.citacomsys.retail.api.dto.RetailMetadataUpdateRequest
import com.citacomsys.retail.api.model.RetailProduct
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class RetailApiService(
    client: HttpClient,
    endpoint: String,
    json: Json
) : KtorProductApiService<RetailProduct>(client, endpoint, json) {

    suspend fun updateProductMetadata(productId: String, request: RetailMetadataUpdateRequest) {
        client.put("$endpoint/$productId/metadata") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
