package com.citacomsys.core.sync.engine.impl

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.sync.CircuitBreakerPolicy
import com.citacomsys.core.sync.CircuitState
import com.citacomsys.core.sync.KmpPriorityQueue
import com.citacomsys.core.sync.QueuedSync
import com.citacomsys.core.sync.SyncSlaRegistry
import com.citacomsys.core.sync.SyncStatus
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.core.sync.handler.DefaultOfflineSyncWorker
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.core.sync.handler.SingleReplayResult
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.database.PendingRequestQueries
import com.citacomsys.device.service.DeviceService
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.sync.SyncPriority
import com.citacomsys.types.BatchItemResult
import com.citacomsys.types.BatchSyncItem
import com.citacomsys.types.EntityType
import com.citacomsys.types.NodeMeshMode
import com.citacomsys.types.NodeRuntimeConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.koin.core.Koin
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class BatchEntitySyncEngine(
    private val defaultWorker: DefaultOfflineSyncWorker,
    private val manager: InventoryDatabaseManager,
    private val telemetryClient: TelemetryLogClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val clock: Clock = Clock.System,
    private val syncPrefs: SyncPreferencesRepository,
    private val deviceService: DeviceService,
    private val sessionManager: AppSessionManager? = null
) : EntitySyncEngine {
    private val TAG = "BatchSyncEngine"

    override val id: String = "batch.entity.sync.engine"
    override val order: Int = 20000
    override val runLevel: StartupRunLevel = StartupRunLevel.POST_ONBOARDING

    private val handlerMutex = Mutex()
    private val handlers = mutableMapOf<EntityType, EntitySyncHandler>()
    private val queuedEntities = mutableSetOf<EntityType>()

    private val circuitMutex = Mutex()
    private val circuitRegistry = mutableMapOf<EntityType, CircuitState>()
    private val circuitPolicy = CircuitBreakerPolicy()
    private val pullMutex = Mutex()
    private val syncQueue = KmpPriorityQueue<QueuedSync>()
    private val waitingRoom = mutableListOf<QueuedSync>()
    private val activePullEntities = mutableSetOf<EntityType>()
    private val completedEntities = mutableSetOf<EntityType>()

    private val _status = MutableSharedFlow<SyncStatus>()
    override val status: SharedFlow<SyncStatus> = _status.asSharedFlow()

    private val _activePullCount = MutableStateFlow(0)
    private val _isUploading = MutableStateFlow(false)

    override val activeSyncCount: StateFlow<Int> = _activePullCount.asStateFlow()
    override val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    override val isSyncing: StateFlow<Boolean> =
        combine(_isUploading, _activePullCount) { uploading, pulling -> uploading || pulling > 0 }
            .stateIn(scope, SharingStarted.Eagerly, false)

    private val semaphore = Semaphore(3)
    private val deviceInfo = deviceService.deviceInfo

    val syncTargets: StateFlow<List<SyncTarget>> = combine(
        syncPrefs.meshSyncMode,
        syncPrefs.syncConfig,
        syncPrefs.activeHubIp
    ) { mode, config, hubIp ->
        val isSatellite = mode == NodeMeshMode.SATELLITE || !hubIp.isNullOrBlank()
        if (isSatellite) {
            config.targets
        } else {
            config.targets.filter { it.isCloudDeployed }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private var hasTriggeredSync = false

    init {
        Napier.d { "Batch Entity SyncEngine initialized" }

        scope.launch {
            syncPrefs.activeHubIp
                .filterNotNull()
                .distinctUntilChanged()
                .collect { hubIp ->
                    Napier.i(tag = TAG) { "Mother Hub discovered at [$hubIp] -> Resuming paused LAN outbox..." }
                    runFullSyncCycle()
                }
        }

        scope.launch {
            syncTargets
                .debounce(2.minutes)
                .collect { targets ->
                    if (hasTriggeredSync && targets.isNotEmpty()) {
                        Napier.i(tag = TAG) { "Sync targets updated: ${targets.size} active targets -> Triggering full sync cycle..." }
                        runFullSyncCycle()
                    }
                }
        }

        scope.launch {
            EventBus.subscribe<EntityChangeEvent>().onEach { entityEvent ->
                val workspaceId = entityEvent.workspaceId ?: entityEvent.organizationId
                val branchId = entityEvent.branchId ?: workspaceId
                val isMatch = syncTargets.value.any { it.workspaceId == workspaceId && it.branchId == branchId }
                if (isMatch) {
                    requestSync(entityEvent)
                }
            }.launchIn(this)
        }
    }

    override suspend fun run(koin: Koin) {
        Napier.i(tag = TAG) { "Platform post-onboarding triggered -> Executing initial batch sync handshake..." }
        runFullSyncCycle()
        hasTriggeredSync = true
    }

    private suspend fun trackSyncEvent(action: String, summary: String) {
        telemetryClient.trackApplicationEvent(
            actorId = "SYSTEM",
            entityType = "DATA_SYNC",
            entityId = "BATCH_ENGINE",
            action = action,
            summary = summary
        )
    }

    private suspend fun isCircuitOpen(type: EntityType): Boolean = circuitMutex.withLock {
        circuitRegistry[type]?.isOpen(clock) ?: false
    }

    private suspend fun recordSyncFailure(type: EntityType) = circuitMutex.withLock {
        val state = circuitRegistry.getOrPut(type) { CircuitState() }
        state.recordFailure(circuitPolicy, clock)
        if (state.isOpen(clock)) {
            Napier.w { "🚨 CIRCUIT OPENED for $type due to consecutive failures." }
            trackSyncEvent(
                "CIRCUIT_OPENED",
                "Synchronization circuit opened for $type until ${state.openUntil}"
            )
        }
    }

    private suspend fun recordSyncSuccess(type: EntityType) = circuitMutex.withLock {
        circuitRegistry[type]?.let {
            if (it.failureCount > 0) {
                it.recordSuccess()
                Napier.i { "✅ CIRCUIT CLOSED for $type following successful operation." }
                trackSyncEvent("CIRCUIT_CLOSED", "Synchronization circuit restored for $type.")
            }
        }
    }

    private fun resolveHandler(request: PendingRequestEntity): EntitySyncHandler? {
        return handlers[request.entityType]
            ?: if (request.endpoint.contains("retail")) handlers[EntityType.RETAIL_PRODUCT]
            else if (request.endpoint.contains("pharmacy")) handlers[EntityType.PHARMACY_PRODUCT]
            else null
    }

    private suspend fun runFullSyncCycle() {
        trackSyncEvent("SYNC_CYCLE_START", "Initiating workspace-scoped synchronization cycle")

        val targets = syncTargets.value

        if (targets.isEmpty()) {
            Napier.i(tag = TAG) { "Sync targets list is empty. Skipping sync cycle." }
            return
        }

        val targetsByWorkspace = targets.groupBy { it.workspaceId }

        for ((workspaceId, wsTargets) in targetsByWorkspace) {
            _isUploading.value = true
            val pushSuccessful = try {
                runBatchReplayPipelineForWorkspace(workspaceId)
            } catch (e: Exception) {
                Napier.w(e) { "Outbox push interrupted for workspace $workspaceId; skipping pull." }
                false
            } finally {
                _isUploading.value = false
            }

            if (!pushSuccessful) {
                Napier.w(tag = TAG) { "Outbox push failed or hub not reached for workspace $workspaceId. Skipping pull for this workspace." }
                continue
            }

            // Consistent Pull for each branch target in this workspace
            try {
                val handlersSnapshot = handlerMutex.withLock { handlers.values.toList() }
                for (target in wsTargets) {
                    for (handler in handlersSnapshot.sortedByDescending { it.priority }) {
                        handler.onSync(listOf(target))
                    }
                }
                updateSuccessfulPullMetadata()
            } catch (e: Exception) {
                recordSyncFailure(EntityType.PRODUCT)
                Napier.e(e) { "Pull failed for workspace $workspaceId" }
            }
        }

        trackSyncEvent("SYNC_CYCLE_COMPLETED", "Workspace-scoped synchronization cycle finished.")
    }

    suspend fun runBatchReplayPipelineForWorkspace(workspaceId: String, batchSize: Long = 50L): Boolean {
        val db = manager.getDatabase()
        val pendingQueries = db.pendingRequestQueries

        pendingQueries.resetStaleSyncing()

        val nodeConfig = NodeRuntimeConfig(
            nodeId = deviceInfo.value.hardwareId,
            name = deviceInfo.value.displayName,
            currentDeploymentMode = if (syncPrefs.isMotherHubEnabled.value) "HUB" else "SATELLITE",
        )

        val activeHubIp = syncPrefs.activeHubIp.value
        val isMotherHub = syncPrefs.isMotherHubEnabled.value
        val meshMode = syncPrefs.meshSyncMode.value

        val isWorkspacePaused = activeHubIp == null && !isMotherHub && (
            meshMode == NodeMeshMode.SATELLITE || syncPrefs.syncConfig.value.targets.any { it.workspaceId == workspaceId && !it.isCloudDeployed }
        )

        if (isWorkspacePaused) {
            Napier.w { "Workspace $workspaceId is LAN-only and Mother Hub is unreachable. Skipping outbox push." }
            return false
        }

        while (true) {
            val batch = pendingQueries.getPendingRequestsForWorkspace(workspaceId, limit = batchSize).executeAsList()
            if (batch.isEmpty()) break

            val networkItems = mutableListOf<BatchSyncItem>()
            val handlerMap = mutableMapOf<String, EntitySyncHandler?>()
            val startTime = clock.now()

            for (request in batch) {
                if (isCircuitOpen(request.entityType)) {
                    Napier.w { "Halting batch at ${request.entityType}: circuit is OPEN." }
                    return false
                }

                val handler = handlerMutex.withLock { resolveHandler(request) }
                handlerMap[request.id] = handler

                val customResult = handler?.onPush(request)
                if (customResult != null) {
                    if (customResult is SingleReplayResult.LanHubUnavailable) {
                        pendingQueries.markAsPending(listOf(request.id))
                        return false
                    }
                    var halt = false
                    handleSingleResult(customResult, pendingQueries, request, startTime) { halt = true }
                    if (halt) return false
                    continue
                }

                networkItems.add(
                    defaultWorker.buildBatchItem(
                        request = request,
                        payloadHandler = handler,
                        nodeConfig = nodeConfig
                    )
                )
            }

            if (networkItems.isEmpty()) break

            pendingQueries.markAsSyncing(networkItems.map { it.queueId })

            val batchResponse = try {
                defaultWorker.sendBatch(networkItems)
            } catch (e: Exception) {
                pendingQueries.markAsPending(networkItems.map { it.queueId })
                Napier.w(e) { "Batch push interrupted by network error for workspace $workspaceId." }
                return false
            }

            val acknowledgedIds = batchResponse.results.map { it.queueId }.toSet()

            for (itemResult in batchResponse.results) {
                val originalRequest = batch.firstOrNull { it.id == itemResult.queueId } ?: continue
                val handler = handlerMap[itemResult.queueId]

                if (itemResult.status in 200..299) {
                    handler?.onSuccessRaw(originalRequest.id, itemResult.body)
                    pendingQueries.deleteById(originalRequest.id)
                    recordSyncSuccess(originalRequest.entityType)
                    updateSuccessfulPushMetadata()
                } else if (itemResult.status == 503 || itemResult.status == 502) {
                    pendingQueries.markAsPending(listOf(originalRequest.id))
                    return false
                } else {
                    var shouldHaltQueue = false
                    handleBatchItemError(
                        itemResult = itemResult,
                        pendingQueries = pendingQueries,
                        request = originalRequest,
                        haltQueue = { shouldHaltQueue = true }
                    )
                    if (shouldHaltQueue) {
                        val unprocessed = networkItems.map { it.queueId }.filterNot { it in acknowledgedIds }
                        if (unprocessed.isNotEmpty()) pendingQueries.markAsPending(unprocessed)
                        return false
                    }
                }
            }
        }
        return true
    }

    private suspend fun handleSingleResult(
        result: SingleReplayResult,
        pendingQueries: PendingRequestQueries,
        request: PendingRequestEntity,
        startTime: Instant,
        haltQueue: () -> Unit
    ) {
        when (result) {
            is SingleReplayResult.Success -> {
                pendingQueries.deleteById(request.id)
                recordSyncSuccess(request.entityType)
                updateSuccessfulPushMetadata()
            }
            is SingleReplayResult.PermanentFailure -> {
                pendingQueries.moveToDeadLetterLog(
                    id = request.id,
                    reason = result.reason,
                    timestamp = clock.now()
                )
                recordSyncFailure(request.entityType)
            }
            is SingleReplayResult.RetriableFailure -> {
                pendingQueries.unmarkAsSyncing(
                    id = request.id,
                    reason = result.reason,
                    timestamp = clock.now()
                )
                recordSyncFailure(request.entityType)
                haltQueue()
            }
            is SingleReplayResult.TenantSuspended -> {
                pendingQueries.unmarkAsSyncing(
                    id = request.id,
                    reason = result.reason,
                    timestamp = clock.now()
                )
                recordSyncFailure(request.entityType)
                haltQueue()
            }
            is SingleReplayResult.LanHubUnavailable -> {
                pendingQueries.markAsPending(listOf(request.id))
                Napier.w(tag = TAG) {
                    "LAN Hub unavailable for workspace [${result.workspaceId}]. Pausing LAN-only outbox without incrementing retries."
                }
            }
            is SingleReplayResult.Exception -> {
                handleSingleResult(result.result, pendingQueries, request, startTime, haltQueue)
            }
        }
    }

    private suspend fun handleBatchItemError(
        itemResult: BatchItemResult,
        pendingQueries: PendingRequestQueries,
        request: PendingRequestEntity,
        haltQueue: () -> Unit
    ) {
        val status = itemResult.status
        val errorMsg = itemResult.error ?: "Status $status"

        recordSyncFailure(request.entityType)
        trackSyncErrorMetadata(errorMsg)

        if (status == 402 || status == 403) {
            pendingQueries.unmarkAsSyncing(
                id = request.id,
                reason = "Tenant Suspended ($status)",
                timestamp = clock.now()
            )
            haltQueue()
        } else if (status in 400..499) {
            pendingQueries.moveToDeadLetterLog(
                id = request.id,
                reason = errorMsg,
                timestamp = clock.now()
            )
            val dependentIds = pendingQueries.getDependentRequests(request.entity_id).executeAsList()
            if (dependentIds.isNotEmpty()) {
                pendingQueries.moveBatchToDeadLetterLog(
                    ids = dependentIds,
                    reason = "Cascading Failure: Dependent parent operation context (${request.entity_id}) was permanently rejected.",
                    timestamp = clock.now()
                )
            }
        } else {
            pendingQueries.unmarkAsSyncing(
                id = request.id,
                reason = errorMsg,
                timestamp = clock.now()
            )
            haltQueue()
        }
    }

    override fun register(handler: EntitySyncHandler) {
        scope.launch {
            handlerMutex.withLock { handlers[handler.entityType] = handler }
        }
    }

    override fun unregister(entityType: EntityType) {
        scope.launch {
            handlerMutex.withLock { handlers.remove(entityType) }
        }
    }

    private suspend fun innerRequestSync(entityType: EntityType) {
        val handler = handlerMutex.withLock { handlers[entityType] } ?: return

        val isAlreadyQueued = handlerMutex.withLock {
            if (queuedEntities.contains(entityType) || activePullEntities.contains(entityType)) true
            else {
                queuedEntities.add(entityType)
                false
            }
        }

        if (isAlreadyQueued) return

        val metadata = syncPrefs.observeLastSyncMetadata().firstOrNull() ?: LastSyncMetadata()
        val lastPull = metadata.lastSuccessfulPullTimestamp
        val sla = SyncSlaRegistry.getPolicy(entityType)

        val urgencyScore = if (lastPull == null) 10.0
        else {
            val staleDuration = clock.now() - lastPull
            staleDuration / sla.maxStale
        }

        syncQueue.put(QueuedSync(entityType, handler.priority, urgencyScore))
    }

    override suspend fun requestSync(event: EntityChangeEvent) {
        val entityType = runCatching { EntityType.valueOf(event.entityType) }.getOrNull() ?: return

        val targets = syncTargets.value
        if (targets.isEmpty()) return

        val wsId = event.workspaceId ?: targets.firstOrNull()?.workspaceId ?: return

        scope.launch {
            _isUploading.value = true
            try {
                runBatchReplayPipelineForWorkspace(wsId)
            } finally {
                _isUploading.value = false
            }
        }

        innerRequestSync(entityType)
    }

    override suspend fun requestSync(entityType: EntityType) {
        val targets = syncTargets.value
        if (targets.isEmpty()) return

        innerRequestSync(entityType)
    }

    override suspend fun syncAll() {
        runFullSyncCycle()
    }

    override fun resetCircuit(entityType: EntityType) {}

    override fun getPriority(entityType: EntityType): SyncPriority =
        handlers[entityType]?.priority ?: SyncPriority.NORMAL

    private fun startPullProcessor() {
        scope.launch {
            while (isActive) {
                val queued = syncQueue.take()
                handlerMutex.withLock { queuedEntities.remove(queued.entityType) }

                val handler = handlerMutex.withLock { handlers[queued.entityType] } ?: continue

                val dependenciesMet = pullMutex.withLock {
                    handler.dependsOn.all { completedEntities.contains(it) }
                }

                if (!dependenciesMet) {
                    pullMutex.withLock {
                        if (waitingRoom.none { it.entityType == queued.entityType }) {
                            waitingRoom.add(queued)
                        }
                    }
                    continue
                }

                semaphore.acquire()
                scope.launch {
                    try {
                        val targets = syncTargets.value
                        val result = handler.onSync(targets)
                        if (result.isSuccess) {
                            pullMutex.withLock {
                                completedEntities.add(queued.entityType)
                                val newlyUnblocked = waitingRoom.filter { waiting ->
                                    waiting.dependsOnAll(completedEntities)
                                }
                                waitingRoom.removeAll(newlyUnblocked)
                                newlyUnblocked.forEach { syncQueue.put(it) }
                            }
                            recordSyncSuccess(queued.entityType)
                            updateSuccessfulPullMetadata()
                        } else {
                            recordSyncFailure(queued.entityType)
                        }
                    } catch (e: Exception) {
                        recordSyncFailure(queued.entityType)
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }
    }

    private fun QueuedSync.dependsOnAll(completed: Set<EntityType>): Boolean {
        val handler = handlers[entityType] ?: return true
        return handler.dependsOn.all { completed.contains(it) }
    }

    private suspend fun updateSuccessfulPushMetadata() {
        val current = syncPrefs.observeLastSyncMetadata().firstOrNull() ?: LastSyncMetadata()
        syncPrefs.saveLastSyncMetadata(
            current.copy(
                lastSuccessfulPushTimestamp = clock.now(),
                lastSystemErrorMessage = null
            )
        )
    }

    private suspend fun updateSuccessfulPullMetadata() {
        val current = syncPrefs.observeLastSyncMetadata().firstOrNull() ?: LastSyncMetadata()
        syncPrefs.saveLastSyncMetadata(
            current.copy(
                lastSuccessfulPullTimestamp = clock.now(),
                lastSystemErrorMessage = null
            )
        )
    }

    private suspend fun trackSyncErrorMetadata(reason: String) {
        val current = syncPrefs.observeLastSyncMetadata().firstOrNull() ?: LastSyncMetadata()
        syncPrefs.saveLastSyncMetadata(
            current.copy(
                lastSystemErrorMessage = reason,
                lastFailureTimestamp = clock.now()
            )
        )
    }
}
