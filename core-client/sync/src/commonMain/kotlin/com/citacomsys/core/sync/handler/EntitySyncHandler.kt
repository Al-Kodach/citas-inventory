package com.citacomsys.core.sync.handler

import com.citacomsys.core.domain.error.LanOnlyWorkspaceHubUnavailableException
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.shared.sync.SyncPriority
import com.citacomsys.types.EntityType
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Unified interface for entity synchronization (Pull and Push).
 */
interface EntitySyncHandler {
    val entityType: EntityType

    val priority: SyncPriority
        get() = SyncPriority.NORMAL

    /**
     * Entity types that this handler depends on.
     * The SyncEngine will ensure these are synced before triggering this handler.
     */
    val dependsOn: Set<EntityType>
        get() = emptySet()

    /**
     * Download fresh entity snapshots and apply updates/deletions.
     */
    suspend fun onSync(targets: List<SyncTarget> = emptyList()): Result<Unit>

    /**
     * Push: Process pending request for this entity type.
     * If returns null, the DefaultOfflineSyncWorker will handle request via standard replay.
     */
    suspend fun onPush(request: PendingRequestEntity): SingleReplayResult? = null
    suspend fun getPayload(request: PendingRequestEntity, json: Json): Any? = null

    suspend fun onSuccess(requestId: String, response: HttpResponse) {}
    suspend fun onSuccessRaw(requestId: String, responseBody: String?) {}
}

sealed interface SingleReplayResult {
    object Success : SingleReplayResult

    data class RetriableFailure(val reason: String) : SingleReplayResult
    data class PermanentFailure(val reason: String) : SingleReplayResult
    /** The cloud firewall rejected the tenant status. The data is valid, but the pipe is frozen. */
    data class TenantSuspended(val code: Int, val reason: String) : SingleReplayResult
    /** LAN-only outbox item paused because the Mother Hub is offline. */
    data class LanHubUnavailable(val workspaceId: String, val reason: String) : SingleReplayResult

    data class Exception(val result: SingleReplayResult): SingleReplayResult, RuntimeException()
}

suspend fun executeSync(block: suspend () -> HttpResponse): SingleReplayResult {
    return try {
        val response = block()
        if (response.status.isSuccess()) return SingleReplayResult.Success

        val statusCode = response.status.value

        when {
            // Subscription / Tenant Standing errors -> Freeze queue, DO NOT PURGE DATA
            statusCode == 403 || statusCode == 402 -> {
                SingleReplayResult.TenantSuspended(statusCode, "SaaS Tenant Standing Suspension ($statusCode)")
            }
            // Real permanent data schema or structural business validation issues (e.g. 400 Bad Request, 422)
            statusCode in 400..499 && statusCode != 408 && statusCode != 429 -> {
                SingleReplayResult.PermanentFailure("Server validation rejected request ($statusCode)")
            }
            // Retriable networking/server/gateway bottlenecks
            else -> {
                SingleReplayResult.RetriableFailure("Temporary server error ($statusCode)")
            }
        }
    } catch (e: Exception) {
        if (e is LanOnlyWorkspaceHubUnavailableException) {
            return SingleReplayResult.LanHubUnavailable(
                workspaceId = e.workspaceId,
                reason = e.message ?: "LAN Hub unavailable"
            )
        }
        if (e is SingleReplayResult.Exception) return e.result
        SingleReplayResult.RetriableFailure(e.message ?: "Network transport failure")
    }
}

