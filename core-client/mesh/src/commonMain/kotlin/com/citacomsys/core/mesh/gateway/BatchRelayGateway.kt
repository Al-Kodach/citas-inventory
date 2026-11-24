package com.citacomsys.core.mesh.gateway

import com.citacomsys.core.mesh.model.ActivityType
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.types.BatchItemResult
import com.citacomsys.types.BatchSyncEnvelope
import com.citacomsys.types.BatchSyncResponse
import com.citacomsys.types.EntityType
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class BatchRelayGateway(
    private val json: Json,
    private val httpClient: HttpClient
) : HubServiceGateway {
    override val domain: String = "sync"
    override val entityType: EntityType? = null

    override fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit) {
        route("sync/batch") {
            post {
                val envelope = call.receive<BatchSyncEnvelope>()
                val results = mutableListOf<BatchItemResult>()
                var hasSuccess = false

                for (item in envelope.items) {
                    val itemResult = try {
                        val ktorMethod = when (item.method.uppercase()) {
                            "POST" -> HttpMethod.Post
                            "PUT" -> HttpMethod.Put
                            "DELETE" -> HttpMethod.Delete
                            else -> HttpMethod.Get
                        }

                        val response = httpClient.request(item.endpoint) {
                            method = ktorMethod
                            contentType(ContentType.Application.Json)
                            headers {
                                append("X-Sync-Metadata", item.metadataBase64)
                                append("X-Entity-Id", item.entityId)
                                item.dependantId?.let { append("X-Dependant-Id", it) }
                            }
                            if (item.payloadJson != null) {
                                setBody(item.payloadJson!!)
                            }
                        }

                        val bodyText = response.bodyAsText()
                        if (response.status.isSuccess()) {
                            hasSuccess = true
                        }

                        BatchItemResult(
                            queueId = item.queueId,
                            idempotencyKey = item.idempotencyKey,
                            status = response.status.value,
                            body = bodyText
                        )
                    } catch (e: Exception) {
                        Napier.e(e) { "Failed to relay batch item ${item.queueId} on Mother Hub" }
                        BatchItemResult(
                            queueId = item.queueId,
                            idempotencyKey = item.idempotencyKey,
                            status = 500,
                            error = e.message ?: "Mother Hub relay error"
                        )
                    }

                    results.add(itemResult)
                    if (itemResult.status >= 500) break
                }

                // Trigger ONE consolidated SSE SYNC_REQUIRED broadcast at the end of the batch
                if (hasSuccess) {
                    broadcastAlert(
                        NodeActivity(
                            bindingId = "MOTHER_HUB",
                            timestamp = Clock.System.now(),
                            action = "SYNC_REQUIRED",
                            summary = "Batch synchronization completed on Mother Hub",
                            type = ActivityType.SYNC_SIGNAL
                        )
                    )
                }

                val responseObj = BatchSyncResponse(results)
                call.respondText(
                    json.encodeToString(BatchSyncResponse.serializer(), responseObj),
                    ContentType.Application.Json
                )
            }
        }
    }
}
