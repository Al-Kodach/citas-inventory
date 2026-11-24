package com.citacomsys.core.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * The definitive, self-contained base class for all API services.
 * Refined to isolate server error messages from internal HTTP metadata.
 */
abstract class BaseApi(
    protected val client: HttpClient,
    protected val json: Json = Json
) {
    protected suspend inline fun <reified T> executeRequest(
        crossinline block: suspend () -> HttpResponse
    ): T {
        val response = try {
            block()
        } catch (e: IOException) {
            throw ApiNetworkException(cause = e)
        }

        if (response.status.isSuccess()) {
            return response.body()
        }

        val bodyText = safeBodyAsText(response)

        // Metadata for debugging, not for display to the user
        val errorInfo = HttpErrorMetadata(
            method = response.request.method,
            url = response.request.url,
            status = response.status,
            requestId = response.headers["X-Request-Id"]
        )

        throw mapToApiException(bodyText, response, errorInfo)
    }

    protected suspend fun safeBodyAsText(response: HttpResponse): String =
        try {
            response.bodyAsText()
        } catch (_: Throwable) {
            ""
        }
}

/**
 * Lightweight container for HTTP error metadata.
 * Removed RuntimeException inheritance to prevent message pollution.
 */
data class HttpErrorMetadata(
    val method: HttpMethod,
    val url: Url,
    val status: HttpStatusCode,
    val requestId: String?
)

