package com.citacomsys.module.inventory.data.remote.api.ktor

import com.citacomsys.core.remote.ApiNetworkException
import com.citacomsys.core.remote.HttpErrorMetadata
import com.citacomsys.core.remote.mapToApiException
import com.citacomsys.types.BaseProduct
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.time.Instant
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.module.inventory.model.ProductSnapshot
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult

/**
 * Single universal service that eliminates duplicate HTTP boilerplate classes.
 * Handled via explicit DI path injection.
 */
open class KtorProductApiService<T : BaseProduct>(
    val client: HttpClient,
    val endpoint: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    // ==============================================================================
    // 1. REUSABLE TYPE-SAFE API WORKFLOW METHODS
    // ==============================================================================

    suspend inline fun <reified R : T> getById(id: String): R {
        return executeRequest {
            client.get("$endpoint/$id")
        }
    }

    suspend inline fun <reified R : T> create(request: R): R {
        return executeRequest {
            client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getProductChanges(
        workspaceId: String,
        sinceTimestamp: Instant? = null
    ): ChangeListResponse<T, String> {
        return executeRequest {
            client.get("$endpoint/sync") {
                url {
                    parameters.append("activeWorkspaceId", workspaceId)
                    sinceTimestamp?.let { parameters.append("since", it.toString()) }
                }
            }
        }
    }


    suspend fun delete(id: String) {
        executeRequest<Unit> {
            client.delete("$endpoint/$id")
        }
    }

    suspend inline fun <reified R : T> getProduct(productId: String): R {
        return executeRequest {
            client.get("$endpoint/$productId")
        }
    }

    suspend fun fetchPaged(query: PageQuery): PageResult<ProductSnapshot> {
        return executeRequest {
            client.post("$endpoint/query") {
                contentType(ContentType.Application.Json)
                setBody(query)
            }
        }
    }

    // ==============================================================================
    // 2. ENCAPSULATED BASE API ENGINE (DUPLICATED INSIDE FOR PROTECTION)
    // ==============================================================================

    /**
     * Public inline execution wrapper. This forces Ktor to catch the
     * reified target type at compile time right inside your ViewModels/Repositories.
     */
    @Suppress("NOTHING_TO_INLINE")
    suspend inline fun <reified V> executeRequest(
        crossinline block: suspend () -> HttpResponse
    ): V {
        val response = try {
            block()
        } catch (e: IOException) {
            throw ApiNetworkException(cause = e)
        }

        if (response.status.isSuccess()) {
            return response.body()
        }

        val bodyText = safeBodyAsText(response)
        val errorInfo = HttpErrorMetadata(
            method = response.request.method,
            url = response.request.url,
            status = response.status,
            requestId = response.headers["X-Request-Id"]
        )

        throw mapToApiException(bodyText, response, errorInfo)
    }

    @PublishedApi
    internal suspend fun safeBodyAsText(response: HttpResponse): String = runCatching {
        response.bodyAsText()
    }.getOrElse { "" }

    // --- RAW HTTP METHODS FOR SYNC REPLAY ---
    suspend inline fun <reified T> createRaw(request: T): HttpResponse {
        return client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteRaw(id: String): HttpResponse {
        return client.delete("$endpoint/$id")
    }
}
