package com.citacomsys.core.notification.otp

import com.citacomsys.core.notification.sms.hubtel.HubtelConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.util.Base64

@Serializable
data class HubtelOtpSendRequest(
    val senderId: String,
    val phoneNumber: String,
    val countryCode: String = "GH"
)

@Serializable
data class HubtelOtpData(
    val requestid: String,
    val prefix: String
)

@Serializable
data class HubtelOtpSendResponse(
    val message: String,
    val code: String,
    val data: HubtelOtpData?
)

@Serializable
data class HubtelVerifyRequest(
    val requestid: String,
    val prefix: String,
    val code: String
)

@Serializable
data class HubtelResendRequest(
    val requestid: String
)

internal class HubtelOtpGateway(
    private val config: HubtelConfig,
    private val httpClient: HttpClient
) : OtpGateway {
    private val logger = KotlinLogging.logger("HUBTEL_OTP_GATEWAY")
    private val baseUrl = "https://api-otp.hubtel.com/otp"

    private fun authHeader(): String {
        return "Basic " + Base64.getEncoder()
            .encodeToString("${config.clientId}:${config.clientSecret}".toByteArray())
    }

    override suspend fun sendOtp(senderId: String, phoneNumber: String, countryCode: String): Result<HubtelOtpSendResponse> = runCatching {
        val request = HubtelOtpSendRequest(senderId = senderId, phoneNumber = phoneNumber, countryCode = countryCode)
        val response: HubtelOtpSendResponse = httpClient.post("$baseUrl/send") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        if (response.code != "0000") {
            error("Hubtel OTP send failed (Code ${response.code}): ${response.message}")
        }
        response
    }

    override suspend fun verifyOtp(requestId: String, prefix: String, code: String): Result<Boolean> = runCatching {
        val request = HubtelVerifyRequest(requestid = requestId, prefix = prefix, code = code)
        val response: HubtelOtpSendResponse = httpClient.post("$baseUrl/verify") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        response.code == "0000"
    }

    override suspend fun resendOtp(requestId: String): Result<Boolean> = runCatching {
        val request = HubtelResendRequest(requestid = requestId)
        val response: HubtelOtpSendResponse = httpClient.post("$baseUrl/resend") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        response.code == "0000"
    }
}
