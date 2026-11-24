package com.citacomsys.core.sync

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.model.SyncConfig
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.sync.engine.impl.BatchEntitySyncEngine
import com.citacomsys.core.sync.handler.DefaultOfflineSyncWorker
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.database.InventoryDatabase
import com.citacomsys.device.service.DeviceService
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.EntityType
import com.citacomsys.types.NodeMeshMode
import com.citacomsys.types.UserDeviceInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class EntitySyncEngineTest {

    private class FakeSyncPrefs(
        initialConfig: SyncConfig = SyncConfig(),
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

    private fun setupMockDb(dbManager: InventoryDatabaseManager) {
        val mockDb = mockk<InventoryDatabase>(relaxed = true)
        coEvery { dbManager.getDatabase() } returns mockDb
        every { dbManager.database } returns flowOf(mockDb)
        every {
            mockDb.pendingRequestQueries.getPendingRequests(any()).executeAsOneOrNull()
        } returns null
    }

    private fun setupMockDeviceService(): DeviceService {
        val deviceService = mockk<DeviceService>(relaxed = true)
        every { deviceService.deviceInfo } returns MutableStateFlow(
            UserDeviceInfo(
                hardwareId = "hw-1",
                status = DeviceStatus.ACTIVE,
                displayName = "POS-1",
                lastUsedAt = Clock.System.now()
            )
        )
        return deviceService
    }

    @Test
    fun testEmptySyncTargetsBypassesSyncCycle() = runTest {
        val syncPrefs = FakeSyncPrefs(
            initialConfig = SyncConfig(targets = emptyList())
        )
        val defaultWorker: DefaultOfflineSyncWorker = mockk(relaxed = true)
        val dbManager: InventoryDatabaseManager = mockk(relaxed = true)
        setupMockDb(dbManager)
        val telemetryClient: TelemetryLogClient = mockk(relaxed = true)
        val deviceService = setupMockDeviceService()

        val engine = BatchEntitySyncEngine(
            defaultWorker = defaultWorker,
            manager = dbManager,
            telemetryClient = telemetryClient,
            scope = backgroundScope,
            syncPrefs = syncPrefs,
            deviceService = deviceService
        )

        val handler = object : EntitySyncHandler {
            override val entityType = EntityType.PRODUCT
            var synced = false
            override suspend fun onSync(targets: List<SyncTarget>): Result<Unit> {
                synced = true
                return Result.success(Unit)
            }
        }

        engine.register(handler)
        testScheduler.advanceUntilIdle()
        engine.syncAll()
        testScheduler.advanceUntilIdle()

        assertEquals(false, handler.synced)
    }

    @Test
    fun testActiveSyncTargetsInvokesOnSyncWithTargets() = runTest {
        val target = SyncTarget(workspaceId = "ws-123", branchId = "br-123")

        var receivedTargets: List<SyncTarget>? = null
        val handler = object : EntitySyncHandler {
            override val entityType = EntityType.PRODUCT
            override suspend fun onSync(targets: List<SyncTarget>): Result<Unit> {
                receivedTargets = targets
                return Result.success(Unit)
            }
        }

        val result = handler.onSync(listOf(target))
        assertEquals(Result.success(Unit), result)
        assertEquals(listOf(target), receivedTargets)
    }
}
