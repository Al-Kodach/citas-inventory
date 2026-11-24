package com.citacomsys.sales.data.remote.api

import com.citacomsys.core.remote.BaseApi
import com.citacomsys.shared.sync.SyncApiResponse
import com.citacomsys.api.sales.Sale
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.api.sales.SalesRecord
import com.citacomsys.api.sales.dto.CreateSaleRequest
import com.citacomsys.api.sales.dto.SaleResult
import com.citacomsys.api.sales.dto.PreviewSaleRequest
import com.citacomsys.api.sales.dto.PreviewResponse
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.time.Clock.System.now

interface SalesApi {
    suspend fun createSale(request: CreateSaleRequest): SaleResult
    suspend fun getSaleDetails(saleId: String): SaleDetails
    suspend fun getSaleRecord(saleId: String): SalesRecord
    suspend fun getSales(): List<Sale>
    suspend fun previewSale(request: PreviewSaleRequest): PreviewResponse

    suspend fun syncOfflineSales(sales: CreateSaleRequest): HttpResponse
}

internal class KtorSalesApi(
    client: HttpClient,
    private val endpoint: String = "api/v1/sales"
) : BaseApi(client), SalesApi {

    override suspend fun createSale(request: CreateSaleRequest): SaleResult {
        return executeRequest {
            client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun getSaleDetails(saleId: String): SaleDetails =
        executeRequest {
            client.get("$endpoint/$saleId")
        }

    override suspend fun getSaleRecord(saleId: String): SalesRecord {
        return executeRequest {
            client.get("$endpoint/$saleId/record")
        }
    }

    override suspend fun getSales(): List<Sale> = executeRequest {
        client.get(endpoint)
    }

    override suspend fun previewSale(request: PreviewSaleRequest): PreviewResponse =
        executeRequest {
            Napier.d { "Sending salePreview request at ${now()}" }
            client.post("$endpoint/previewInvoice") {
                setBody(request)
            }
        }

    override suspend fun syncOfflineSales(sales: CreateSaleRequest): HttpResponse {
        return client.post("$endpoint/sync") {
            contentType(ContentType.Application.Json)
            setBody(sales)
        }
    }
}
