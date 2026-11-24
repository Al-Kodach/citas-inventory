package com.citacomsys.pharmacy.shared.data.remote

import com.citacomsys.module.inventory.data.remote.api.ktor.KtorProductApiService
import com.citacomsys.pharmacy.api.dto.PharmacyMetadataUpdateRequest
import com.citacomsys.pharmacy.api.model.PharmacyProduct
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.Json

class PharmacyApiService(
    client: HttpClient,
    endpoint: String,
    json: Json
): KtorProductApiService<PharmacyProduct>(client, endpoint, json) {

    suspend fun updateProductMetadataRaw(
        productId: String,
        request: PharmacyMetadataUpdateRequest
    ): HttpResponse {
        return client.put("$endpoint/$productId/metadata") {
            setBody(request)
        }
    }
}