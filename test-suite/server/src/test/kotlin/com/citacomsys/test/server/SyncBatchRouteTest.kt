package com.citacomsys.test.server

import com.citacomsys.core.domain.sync.SyncEntityDispatcher
import com.citacomsys.core.persistence.route.syncBatchRoutes
import com.citacomsys.core.persistence.table.sync.ProcessedSyncEventsTable
import com.citacomsys.types.BatchSyncEnvelope
import com.citacomsys.types.BatchSyncItem
import com.citacomsys.types.BatchSyncResponse
import com.citacomsys.types.DispatchOutcome
import com.citacomsys.types.SyncMetadata
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalEncodingApi::class)
class SyncBatchRouteTest {

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test_batch_sync_${UUID.randomUUID()};DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(ProcessedSyncEventsTable)
        }
    }

    private fun createMetadataBase64(): String {
        val meta = SyncMetadata(
            wsId = UUID.randomUUID().toString(),
            brId = UUID.randomUUID().toString(),
            actorId = "actor-1",
            actorName = "Actor One",
            createdAt = "2025-01-01T00:00:00Z"
        )
        val jsonStr = Json.encodeToString(SyncMetadata.serializer(), meta)
        return Base64.encode(jsonStr.toByteArray())
    }

    @Test
    fun testSyncBatchSuccessAndIdempotencyGuard() = testApplication {
        application {
            install(ServerContentNegotiation) {
                json()
            }
        }

        val metaBase64 = createMetadataBase64()
        var dispatchCount = 0

        val fakeDispatcher = object : SyncEntityDispatcher {
            override suspend fun dispatch(item: BatchSyncItem, metadata: SyncMetadata): DispatchOutcome {
                dispatchCount++
                return DispatchOutcome(status = 201, body = "{\"id\":\"${item.entityId}\"}")
            }
        }

        routing {
            syncBatchRoutes(fakeDispatcher)
        }

        val client = createClient {
            install(ClientContentNegotiation) {
                json()
            }
        }

        val item1 = BatchSyncItem(
            queueId = "req-1",
            idempotencyKey = "key-1",
            entityType = "PRODUCT",
            entityId = "prod-1",
            method = "POST",
            endpoint = "/api/v1/inventory/products",
            metadataBase64 = metaBase64
        )

        // First execution -> dispatches and records idempotency receipt
        val response1 = client.post("/api/v1/sync/batch") {
            contentType(ContentType.Application.Json)
            setBody(BatchSyncEnvelope(listOf(item1)))
        }

        assertEquals(HttpStatusCode.OK, response1.status)
        val body1: BatchSyncResponse = response1.body()
        assertEquals(1, body1.results.size)
        assertEquals(201, body1.results[0].status)
        assertEquals(1, dispatchCount)

        // Re-send same item -> idempotency guard returns stored receipt without dispatching business logic again
        val response2 = client.post("/api/v1/sync/batch") {
            contentType(ContentType.Application.Json)
            setBody(BatchSyncEnvelope(listOf(item1)))
        }

        assertEquals(HttpStatusCode.OK, response2.status)
        val body2: BatchSyncResponse = response2.body()
        assertEquals(1, body2.results.size)
        assertEquals(201, body2.results[0].status)
        assertEquals(1, dispatchCount) // Remains 1!
    }

    @Test
    fun testSyncBatchStopsEarlyOnServerError500() = testApplication {
        application {
            install(ServerContentNegotiation) {
                json()
            }
        }

        val metaBase64 = createMetadataBase64()
        var dispatchCount = 0

        val fakeDispatcher = object : SyncEntityDispatcher {
            override suspend fun dispatch(item: BatchSyncItem, metadata: SyncMetadata): DispatchOutcome {
                dispatchCount++
                if (item.queueId == "req-1") {
                    throw RuntimeException("Database timeout")
                }
                return DispatchOutcome(status = 200, body = "ok")
            }
        }

        routing {
            syncBatchRoutes(fakeDispatcher)
        }

        val client = createClient {
            install(ClientContentNegotiation) {
                json()
            }
        }

        val item1 = BatchSyncItem(
            queueId = "req-1",
            idempotencyKey = "key-1",
            entityType = "PRODUCT",
            entityId = "prod-1",
            method = "POST",
            endpoint = "/api/v1/inventory/products",
            metadataBase64 = metaBase64
        )

        val item2 = BatchSyncItem(
            queueId = "req-2",
            idempotencyKey = "key-2",
            entityType = "PRODUCT",
            entityId = "prod-2",
            method = "POST",
            endpoint = "/api/v1/inventory/products",
            metadataBase64 = metaBase64
        )

        val response = client.post("/api/v1/sync/batch") {
            contentType(ContentType.Application.Json)
            setBody(BatchSyncEnvelope(listOf(item1, item2)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body: BatchSyncResponse = response.body()
        assertEquals(1, body.results.size) // Stopped early at req-1
        assertEquals(500, body.results[0].status)
        assertEquals(1, dispatchCount)
    }
}
