package com.citacomsys.feature.telemetry

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.device.service.DeviceService
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.feature.api.telemetry.dao.LocalTelemetryDao
import com.citacomsys.feature.api.telemetry.dto.ApplicationEventDTO
import com.citacomsys.feature.api.telemetry.dto.IdentityEventDTO
import com.citacomsys.feature.api.telemetry.model.ClientLogSyncBatch
import com.citacomsys.types.generateUUID
import io.github.aakira.napier.LogLevel
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

internal class KmpTelemetryLogClient(
    private val httpClient: HttpClient,
    private val deviceService: DeviceService,
    private val sessionManager: AppSessionManager,
    private val localDao: LocalTelemetryDao,
    private val json: Json = Json,
    private val scope: CoroutineScope,
) : TelemetryLogClient {

    override suspend fun trackIdentityEvent(
        userId: String,
        action: String,
        outcome: String,
        reason: String?,
        workspaceId: String?,
        branchId: String?,
    ) {
        val now = Clock.System.now().toString()
        val payload = json.encodeToString(
            mapOf("outcome" to outcome, "failureReason" to reason.orEmpty())
        )
        val effectiveWsId = workspaceId?.takeIf { it.isNotBlank() } ?: sessionManager.activeWorkspaceId.orEmpty()
        val effectiveBranchId = branchId?.takeIf { it.isNotBlank() } ?: runCatching { sessionManager.resolveWriteBranchId() }.getOrNull()

        localDao.insertLog(
            id = generateUUID(),
            logSource = "IDENTITY",
            actionOrEvent = action,
            level = "INFO",
            workspaceId = effectiveWsId,
            branchId = effectiveBranchId,
            userId = userId,
            payload = payload,
            createdAt = now
        )
    }

    override suspend fun trackApplicationEvent(
        actorId: String,
        entityType: String,
        entityId: String,
        action: String,
        summary: String,
        workspaceId: String?,
        branchId: String?,
    ) {
        val now = Clock.System.now().toString()
        val payload = json.encodeToString(
            mapOf("entityType" to entityType, "entityId" to entityId, "summary" to summary)
        )
        val effectiveWsId = workspaceId?.takeIf { it.isNotBlank() } ?: sessionManager.activeWorkspaceId.orEmpty()
        val effectiveBranchId = branchId?.takeIf { it.isNotBlank() } ?: runCatching { sessionManager.resolveWriteBranchId() }.getOrNull()

        localDao.insertLog(
            id = generateUUID(),
            logSource = "APPLICATION",
            actionOrEvent = action,
            level = "INFO",
            workspaceId = effectiveWsId,
            branchId = effectiveBranchId,
            userId = actorId,
            payload = payload,
            createdAt = now
        )
    }

    override fun log(
        level: LogLevel,
        message: String,
        action: String,
        workspaceId: String?,
        branchId: String?,
    ) {
        val user = sessionManager.appUser
        val now = Clock.System.now().toString()
        val effectiveWsId = workspaceId?.takeIf { it.isNotBlank() } ?: sessionManager.activeWorkspaceId.orEmpty()
        val effectiveBranchId = branchId?.takeIf { it.isNotBlank() } ?: runCatching { sessionManager.resolveWriteBranchId() }.getOrNull()

        scope.launch {
            localDao.insertLog(
                id = generateUUID(),
                logSource = "APPLICATION",
                actionOrEvent = action,
                level = level.name,
                workspaceId = effectiveWsId,
                branchId = effectiveBranchId,
                userId = user.value?.name,
                payload = message,
                createdAt = now
            )
        }
    }

    override suspend fun syncLogsToServer(
        targetSources: Set<String>,
        maxAgeWindow: Instant?,
        chunkLimit: Long
    ): Result<Boolean> = runCatching {
        val allowedSources = targetSources.ifEmpty { setOf("IDENTITY", "APPLICATION") }
        val timeCutoff = (maxAgeWindow ?: Clock.System.now()).toString()

        val rawLogs = localDao.getLogsForSync(
            allowedSources = allowedSources.toList(),
            timeCutoff = timeCutoff,
            limit = chunkLimit
        )

        if (rawLogs.isEmpty()) return Result.success(true)

        val logIds = rawLogs.map { it.id }
        localDao.markAsSyncing(logIds)

        val syncBatch = ClientLogSyncBatch(
            deviceId = deviceService.getDeviceId(),
            appFlavor = sessionManager.activeProductType.name,
            identityLogs = rawLogs.filter { it.logSource == "IDENTITY" }.map { log ->
                IdentityEventDTO(
                    id = log.id,
                    action = log.actionOrEvent,
                    level = log.level,
                    createdAt = Instant.parse(log.createdAt),
                    userId = log.userId.orEmpty(),
                    branchId = log.branchId,
                    payloadData = log.payload
                )
            },
            applicationLogs = rawLogs.filter { it.logSource == "APPLICATION" }.map { log ->
                ApplicationEventDTO(
                    id = log.id,
                    action = log.actionOrEvent,
                    level = log.level,
                    createdAt = Instant.parse(log.createdAt),
                    userId = log.userId,
                    branchId = log.branchId,
                    payloadData = log.payload
                )
            }
        )

        val response = httpClient.post("/api/v1/telemetry/sync") {
            contentType(ContentType.Application.Json)
            setBody(syncBatch)
        }

        if (response.status == HttpStatusCode.OK) {
            localDao.deleteSyncedLogs(logIds)
            true
        } else {
            localDao.unmarkAsSyncing(logIds)
            false
        }
    }
}
