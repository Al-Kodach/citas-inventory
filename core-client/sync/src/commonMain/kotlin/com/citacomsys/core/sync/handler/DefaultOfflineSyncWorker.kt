package com.citacomsys.core.sync.handler

import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.core.remote.SkipClientDefaultsKey
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.types.BatchSyncEnvelope
import com.citacomsys.types.BatchSyncItem
import com.citacomsys.types.BatchSyncResponse
import com.citacomsys.types.MeshHop
import com.citacomsys.types.NodeRuntimeConfig
import com.citacomsys.types.SyncMetadata
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

/**
 * A generic sync worker that intercepts and wraps all outbox sync requests
 * into EntityPushSyncRequest with complete metadata and routing payloads.
 */
class DefaultOfflineSyncWorker(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    private val TAG = "OFFLINE_SYNC_WORKER"
    suspend fun processSingle(
        request: PendingRequestEntity,
        payloadHandler: EntitySyncHandler?,
        nodeConfig: NodeRuntimeConfig
    ): SingleReplayResult {
        return executeSync {
            val ktorMethod = when (request.method) {
                RequestMethod.POST -> HttpMethod.Post
                RequestMethod.PUT -> HttpMethod.Put
                RequestMethod.DELETE -> HttpMethod.Delete
                RequestMethod.GET -> HttpMethod.Get
            }

            // Reconstruct or initiate the chain history
            val existingChain = request.routing_chain_json?.let {
                json.decodeFromString<List<MeshHop>>(it)
            } ?: emptyList()

            // Append this node's footprint as a node hop in the graph
            val updatedChain = existingChain + MeshHop(
                nodeId = nodeConfig.nodeId,
                nodeName = nodeConfig.name,
                replayedAt = Clock.System.now().toString(),
                channelMode = nodeConfig.currentDeploymentMode // "SATELLITE" or "HUB"
            )

            // Construct compact metadata
            val metadata = SyncMetadata(
                wsId = request.workspace_id,
                brId = request.branch_id,
                actorId = request.actor_id,
                actorName = request.actor_name,
                createdAt = request.createdAt,
                routingChain = updatedChain
            )

            // Serialize to JSON, then stringify cleanly via Base64
            val jsonStr = json.encodeToString(SyncMetadata.serializer(), metadata)
            val encodedMetadata = Base64.encode(jsonStr.toByteArray(Charsets.UTF_8))

            val payload = request.payload ?: run {
                // Get the protected/encrypted payload from the handler
                payloadHandler?.getPayload(request, json)
            }

            val response = httpClient.request(request.endpoint) {
                method = ktorMethod
                contentType(ContentType.Application.Json)
                attributes.put(SkipClientDefaultsKey, true)
                headers {
                    append("X-Sync-Metadata", encodedMetadata)
                    append("X-Entity-Id", request.entity_id)
                    request.dependant_id?.let { append("X-Dependant-Id", it) }
                }
                if (payload != null) {
                    setBody(payload)
                }
            }

            if (response.status.isSuccess()) {
                payloadHandler?.onSuccess(request.id, response)
            }

            response
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun buildBatchItem(
        request: PendingRequestEntity,
        payloadHandler: EntitySyncHandler?,
        nodeConfig: NodeRuntimeConfig
    ): BatchSyncItem {
        val existingChain = request.routing_chain_json?.let {
            json.decodeFromString<List<MeshHop>>(it)
        } ?: emptyList()

        val updatedChain = existingChain + MeshHop(
            nodeId = nodeConfig.nodeId,
            nodeName = nodeConfig.name,
            replayedAt = Clock.System.now().toString(),
            channelMode = nodeConfig.currentDeploymentMode
        )

        val metadata = SyncMetadata(
            wsId = request.workspace_id,
            brId = request.branch_id,
            actorId = request.actor_id,
            actorName = request.actor_name,
            createdAt = request.createdAt,
            routingChain = updatedChain
        )

        val jsonStr = json.encodeToString(SyncMetadata.serializer(), metadata)
        val encodedMetadata = Base64.encode(jsonStr.toByteArray(Charsets.UTF_8))

        val payload = request.payload ?: run {
            payloadHandler?.getPayload(request, json)?.let {
                if (it is String) it else json.encodeToString(
                    JsonElement.serializer(), json.encodeToJsonElement(
                        serializer(), it))
            }
        }

        return BatchSyncItem(
            queueId = request.id,
            idempotencyKey = "${nodeConfig.nodeId}_${request.id}",
            entityType = request.entityType.name,
            entityId = request.entity_id,
            dependantId = request.dependant_id,
            method = request.method.name,
            endpoint = request.endpoint,
            metadataBase64 = encodedMetadata,
            payloadJson = payload
        )
    }

    suspend fun sendBatch(items: List<BatchSyncItem>): BatchSyncResponse {
        val envelope = BatchSyncEnvelope(items)
        val response = httpClient.post("/api/v1/sync/batch") {
            contentType(ContentType.Application.Json)
            attributes.put(SkipClientDefaultsKey, true)
            setBody(envelope)
        }
        return response.body()
    }
}
