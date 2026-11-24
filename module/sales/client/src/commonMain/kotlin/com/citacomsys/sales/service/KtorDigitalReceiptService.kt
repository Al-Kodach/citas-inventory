package com.citacomsys.sales.service

import com.citacomsys.core.remote.BaseApi
import com.citacomsys.api.sales.service.DigitalReceiptService
import com.citacomsys.types.PhoneNumber
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class KtorDigitalReceiptService(
    client: HttpClient,
    private val endpoint: String = "api/v1/sales"
) : BaseApi(client),
    DigitalReceiptService {
    override suspend fun sendSmsReceipt(
        saleId: String,
        phone: PhoneNumber,
        workspaceId: String
    ): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/$saleId/receipt/sms") {
                contentType(ContentType.Application.Json)
                setBody(phone)
            }
        }
    }

    override suspend fun sendReceipt(
        phone: String,
        receipt: String,
        workspaceId: String
    ): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/receipt-txt/sms") {
                setBody(mapOf("phone" to phone, "receipt" to receipt))
            }
        }
    }
}
