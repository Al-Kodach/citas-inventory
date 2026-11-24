package com.citacomsys.core.persistence.route

import com.citacomsys.core.domain.sync.SyncEntityDispatcher
import com.citacomsys.core.persistence.table.sync.ProcessedSyncEventsTable
import com.citacomsys.types.BatchItemResult
import com.citacomsys.types.BatchSyncEnvelope
import com.citacomsys.types.BatchSyncResponse
import com.citacomsys.types.DispatchOutcome
import com.citacomsys.types.SyncMetadata
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Route.syncBatchRoutes(
    dispatcher: SyncEntityDispatcher,
    json: Json = Json { ignoreUnknownKeys = true }
) {
    post("/api/v1/sync/batch") {
        val envelope = call.receive<BatchSyncEnvelope>()
        val results = mutableListOf<BatchItemResult>()

        for (item in envelope.items) {
            val itemResult = transaction {
                // Check idempotency guard first
                val existing = ProcessedSyncEventsTable
                    .selectAll()
                    .where { ProcessedSyncEventsTable.eventId eq item.idempotencyKey }
                    .singleOrNull()

                if (existing != null) {
                    return@transaction BatchItemResult(
                        queueId = item.queueId,
                        idempotencyKey = item.idempotencyKey,
                        status = existing[ProcessedSyncEventsTable.httpStatus],
                        body = existing[ProcessedSyncEventsTable.responseBody]
                    )
                }

                // Decode SyncMetadata (for workspaceId, actorId, routingChain)
                val metadataJson = Base64.decode(item.metadataBase64).decodeToString()
                val metadata = json.decodeFromString<SyncMetadata>(metadataJson)

                // Execute your existing domain handler (Product, Batch, Sale, etc.)
                val execResult = try {
                    runBlocking { dispatcher.dispatch(item, metadata) }
                } catch (e: IllegalArgumentException) {
                    DispatchOutcome(status = 422, error = e.message ?: "Validation failed")
                } catch (e: Exception) {
                    rollback()
                    return@transaction BatchItemResult(
                        queueId = item.queueId,
                        idempotencyKey = item.idempotencyKey,
                        status = 500,
                        error = e.message ?: "Server error"
                    )
                }

                // Record idempotency receipt inside the SAME transaction
                if (execResult.status in 200..299 || execResult.status == 409) {
                    ProcessedSyncEventsTable.insert {
                        it[eventId] = item.idempotencyKey
                        it[workspaceId] = UUID.fromString(metadata.wsId)
                        it[entityType] = item.entityType
                        it[entityId] = item.entityId
                        it[httpStatus] = execResult.status
                        it[responseBody] = execResult.body
                    }
                }

                BatchItemResult(
                    queueId = item.queueId,
                    idempotencyKey = item.idempotencyKey,
                    status = execResult.status,
                    body = execResult.body,
                    error = execResult.error
                )
            }

            results.add(itemResult)

            // Stop batch early on transient server/DB failure (5xx) so dependent items don't cascade-fail
            if (itemResult.status >= 500) break
        }

        call.respond(HttpStatusCode.OK, BatchSyncResponse(results))
    }
}
