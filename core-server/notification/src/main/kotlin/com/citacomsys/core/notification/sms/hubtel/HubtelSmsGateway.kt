package com.citacomsys.core.notification.sms.hubtel

import com.citacomsys.core.notification.sms.SmsGateway
import com.citacomsys.core.notification.sms.notification.SmsResult
import com.citacomsys.core.notification.sms.notification.SmsStatusResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.Base64

@Serializable
private data class HubtelSmsRequest(
    val from: String,
    val to: String,
    val content: String
)

@Serializable
private data class HubtelSendResponse(
    val rate: Float,
    val messageId: String,
    val status: Int,
    val statusDescription: String?,
    val networkId: String?
)

// DTO for the STATUS CHECK response
@Serializable
private data class HubtelStatusResponse(
    val rate: Float,
    val units: Int,
    val messageId: String,
    val content: String?,
    val status: String?, // "Sent", "Rejected", "Delivered"
    val clientReference: String?,
    val networkId: String?,
    val updateTime: String?,
    val time: String?,
    val to: String?,
    val from: String?
)

internal class HubtelSmsGateway(
    private val config: HubtelConfig,
    private val httpClient: HttpClient
) : SmsGateway {

    private val endpoint = "https://smsc.hubtel.com/v1/messages"

    init {
        logger.info { "Initializing Hubtel Sms Service" }
    }

    override suspend fun send(phone: String, message: String): SmsResult =
        withContext(Dispatchers.IO) {
            val request = HubtelSmsRequest(
                from = config.senderId,
                to = phone,
                content = message
            )

            try {
                val response: HubtelSendResponse = httpClient.post("$endpoint/send") {
                    header(
                        HttpHeaders.Authorization,
                        "Basic " + Base64.getEncoder()
                            .encodeToString("${config.clientId}:${config.clientSecret}".toByteArray())
                    )
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()

                SmsResult(
                    messageId = response.messageId,
                    rate = response.rate,
                    statusDescription = response.statusDescription,
                    status = response.status,
                    networkId = response.networkId ?: ""
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to send SMS due to network or server error." }
                SmsResult(
                    rate = -1f, // A custom rate for "network error"
                    status = -1, // A custom code for "network error"
                    statusDescription = e.message ?: "Unknown network error",
                    messageId = "",
                    networkId = ""
                )
            }
        }

    override suspend fun getStatus(messageId: String): Result<SmsStatusResponse> =
        runCatching {
            withContext(Dispatchers.IO) {
                val status = httpClient.post("$endpoint/$messageId") {
                    header(
                        HttpHeaders.Authorization,
                        "Basic " + Base64.getEncoder()
                            .encodeToString("${config.senderId}:${config.clientSecret}".toByteArray())
                    )
                    contentType(ContentType.Application.Json)
                }.body<HubtelStatusResponse>()

                SmsStatusResponse(
                    rate = status.rate,
                    units = status.units,
                    messageId = status.messageId,
                    content = status.content,
                    status = status.status,
                    clientReference = status.clientReference,
                    networkId = status.networkId,
                    updateTime = status.updateTime,
                    time = status.time,
                    to = status.to,
                    from = status.from
                )
            }
        }
}
