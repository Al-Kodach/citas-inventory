package com.citacomsys.core.sync

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.model.SyncConfig
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.sync.engine.impl.BatchEntitySyncEngine
import com.citacomsys.core.sync.handler.DefaultOfflineSyncWorker
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.database.InventoryDatabase
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.database.PendingRequestQueries
import com.citacomsys.device.service.DeviceService
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.types.BatchItemResult
import com.citacomsys.types.BatchSyncItem
import com.citacomsys.types.BatchSyncResponse
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.EntityType
import com.citacomsys.types.NodeMeshMode
import com.citacomsys.types.UserDeviceInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class BatchEntitySyncEngineTest {

    private class FakeSyncPrefs(
        initialConfig: SyncConfig = SyncConfig(
            targets = listOf(SyncTarget("00000000-0000-0000-0000-000000000001", "br-1"))
        ),
        initialHub: Boolean = false
    ) : SyncPreferencesRepository {
        val configFlow = MutableStateFlow(initialConfig)
        val hubFlow = MutableStateFlow(initialHub)

        override val meshSyncMode: StateFlow<NodeMeshMode> = MutableStateFlow(NodeMeshMode.NODE)
        override val isMotherHubEnabled: StateFlow<Boolean> = hubFlow
        override val syncConfig: StateFlow<SyncConfig> = configFlow

        override val activeHubIp: StateFlow<String?> = MutableStateFlow(null)
        override val activeHubPort: StateFlow<Int> = MutableStateFlow(8443)
        override val activeHubOrgId: StateFlow<String?> = MutableStateFlow(null)
        override val activeHubOrgToken: StateFlow<String?> = MutableStateFlow(null)
        override val activeHubTlsFingerprint: StateFlow<String?> = MutableStateFlow(null)

        override fun saveLastSyncMetadata(metadata: LastSyncMetadata) {}
        override fun observeLastSyncMetadata() = flowOf(null)
        override fun setLastEventSeq(seq: Long) {}
        override fun getLastEventSeq(): Long = 0L

        override fun setMeshSyncMode(mode: NodeMeshMode) {}
        override fun setMotherHubEnabled(enabled: Boolean) {
            hubFlow.value = enabled
        }

        override fun setSyncConfig(config: SyncConfig) {
            configFlow.value = config
        }

        override fun setActiveHubInfo(
            ip: String?,
            port: Int,
            orgId: String?,
            token: String?,
            tlsFingerprint: String?
        ) {
        }
    }

    private fun createDummyRequest(id: String, entityId: String): PendingRequestEntity {
        return PendingRequestEntity(
            id = id,
            payload = "{\"name\":\"Product A\"}",
            workspace_id = "00000000-0000-0000-0000-000000000001",
            branch_id = "00000000-0000-0000-0000-000000000001",
            endpoint = "/api/v1/inventory/products",
            method = RequestMethod.POST,
            entityType = EntityType.PRODUCT,
            entity_id = entityId,
            dependant_id = null,
            eventAction = "CREATE",
            isSyncing = false,
            attempts = 0,
            actor_id = "actor-1",
            actor_name = "Actor 1",
            routing_chain_json = null,
            createdAt = "2025-01-01T00:00:00Z",
            lastFailureReason = null,
            lastFailureTimestamp = null,
            canRetry = true
        )
    }

    @Test
    fun testSuccessfulBatchReplayDeletesRequestAndInvokesOnSuccessRaw() = runTest {
        val syncPrefs = FakeSyncPrefs()
        val defaultWorker: DefaultOfflineSyncWorker = mockk(relaxed = true)
        val dbManager: InventoryDatabaseManager = mockk(relaxed = true)
        val mockDb = mockk<InventoryDatabase>(relaxed = true)
        val mockQueries = mockk<PendingRequestQueries>(relaxed = true)
        val telemetryClient: TelemetryLogClient = mockk(relaxed = true)
        val deviceService: DeviceService = mockk(relaxed = true)

        coEvery { dbManager.getDatabase() } returns mockDb
        every { mockDb.pendingRequestQueries } returns mockQueries
        every { deviceService.deviceInfo } returns MutableStateFlow(
            UserDeviceInfo(
                hardwareId = "hw-1",
                status = DeviceStatus.ACTIVE,
                displayName = "POS-1",
                lastUsedAt = Clock.System.now()
            )
        )

        val req1 = createDummyRequest("req-1", "prod-1")
        var queryCalls = 0
        every { mockQueries.getPendingRequestsForWorkspace(any(), any()).executeAsList() } answers {
            if (queryCalls++ == 0) listOf(req1) else emptyList()
        }

        coEvery { defaultWorker.buildBatchItem(any(), any(), any()) } returns BatchSyncItem(
            queueId = "req-1",
            idempotencyKey = "hw-1_req-1",
            entityType = "PRODUCT",
            entityId = "prod-1",
            method = "POST",
            endpoint = "/api/v1/inventory/products",
            metadataBase64 = "eyJ3c0lkIjoiMSJ9"
        )

        coEvery { defaultWorker.sendBatch(any()) } returns BatchSyncResponse(
            results = listOf(
                BatchItemResult(
                    queueId = "req-1",
                    idempotencyKey = "hw-1_req-1",
                    status = 201,
                    body = "{\"id\":\"prod-1\"}"
                )
            )
        )

        var onSuccessRawCalled = false
        val handler = object : EntitySyncHandler {
            override val entityType = EntityType.PRODUCT
            override suspend fun onSync(targets: List<SyncTarget>) = Result.success(Unit)
            override suspend fun onSuccessRaw(requestId: String, responseBody: String?) {
                onSuccessRawCalled = true
                assertEquals("req-1", requestId)
            }
        }

        val engine = BatchEntitySyncEngine(
            defaultWorker = defaultWorker,
            manager = dbManager,
            telemetryClient = telemetryClient,
            syncPrefs = syncPrefs,
            deviceService = deviceService
        )
        engine.register(handler)

        engine.runBatchReplayPipelineForWorkspace("00000000-0000-0000-0000-000000000001")

        assertTrue(onSuccessRawCalled)
        coVerify { mockQueries.deleteById("req-1") }
    }

    @Test
    fun testBatchReplayHandlesItemFailureAndDeadLettersPoisonItem() = runTest {
        val syncPrefs = FakeSyncPrefs()
        val defaultWorker: DefaultOfflineSyncWorker = mockk(relaxed = true)
        val dbManager: InventoryDatabaseManager = mockk(relaxed = true)
        val mockDb = mockk<InventoryDatabase>(relaxed = true)
        val mockQueries = mockk<PendingRequestQueries>(relaxed = true)
        val telemetryClient: TelemetryLogClient = mockk(relaxed = true)
        val deviceService: DeviceService = mockk(relaxed = true)

        coEvery { dbManager.getDatabase() } returns mockDb
        every { mockDb.pendingRequestQueries } returns mockQueries
        every { deviceService.deviceInfo } returns MutableStateFlow(
            UserDeviceInfo(
                hardwareId = "hw-1",
                status = DeviceStatus.ACTIVE,
                displayName = "POS-1",
                lastUsedAt = Clock.System.now()
            )
        )

        val req1 = createDummyRequest("req-1", "prod-1")
        var queryCalls = 0
        every { mockQueries.getPendingRequestsForWorkspace(any(), any()).executeAsList() } answers {
            if (queryCalls++ == 0) listOf(req1) else emptyList()
        }

        coEvery { defaultWorker.buildBatchItem(any(), any(), any()) } returns BatchSyncItem(
            queueId = "req-1",
            idempotencyKey = "hw-1_req-1",
            entityType = "PRODUCT",
            entityId = "prod-1",
            method = "POST",
            endpoint = "/api/v1/inventory/products",
            metadataBase64 = "eyJ3c0lkIjoiMSJ9"
        )

        coEvery { defaultWorker.sendBatch(any()) } returns BatchSyncResponse(
            results = listOf(
                BatchItemResult(
                    queueId = "req-1",
                    idempotencyKey = "hw-1_req-1",
                    status = 422,
                    error = "Validation failed: invalid SKU"
                )
            )
        )

        val engine = BatchEntitySyncEngine(
            defaultWorker = defaultWorker,
            manager = dbManager,
            telemetryClient = telemetryClient,
            syncPrefs = syncPrefs,
            deviceService = deviceService
        )

        engine.runBatchReplayPipelineForWorkspace("00000000-0000-0000-0000-000000000001")

        coVerify {
            mockQueries.moveToDeadLetterLog(
                id = "req-1",
                reason = "Validation failed: invalid SKU",
                timestamp = any()
            )
        }
    }
}
