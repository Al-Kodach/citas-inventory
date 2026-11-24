package com.citacomsys.sales.data.remote.api

import com.citacomsys.core.remote.BaseApi
import com.citacomsys.database.ProductListItemView
import com.citacomsys.types.ChangeListResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlin.time.Instant

internal interface InventoryQueryApi {
    suspend fun getUpdatedProductsSince(since: Instant?): ChangeListResponse<ProductListItemView, String>
}

internal class KtorInventoryQueryApi(
    client: HttpClient,
    private val endpoint: String = "api/v1/sellable"
) : BaseApi(client), InventoryQueryApi {

    override suspend fun getUpdatedProductsSince(since: Instant?): ChangeListResponse<ProductListItemView, String> {
        return executeRequest {
            client.get(endpoint) {
                since?.let {
                    url {
                        parameters.append("since", since.toString())
                    }
                }
            }
        }
    }
}
