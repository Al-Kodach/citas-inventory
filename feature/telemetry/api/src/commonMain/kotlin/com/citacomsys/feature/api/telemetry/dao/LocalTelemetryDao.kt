package com.citacomsys.feature.api.telemetry.dao

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for local telemetry persistence.
 */
interface LocalTelemetryDao {
    /**
     * Persists a single log entry to local storage.
     */
    suspend fun insertLog(
        id: String,
        logSource: String,
        actionOrEvent: String,
        level: String,
        workspaceId: String?,
        branchId: String?,
        userId: String?,
        payload: String,
        createdAt: String
    )

    /**
     * Retrieves a chunk of logs ready for cloud synchronization.
     */
    suspend fun getLogsForSync(
        allowedSources: List<String>,
        timeCutoff: String,
        limit: Long
    ): List<LocalLogEntry>

    /**
     * Marks a set of logs as currently in-transit to the cloud.
     */
    suspend fun markAsSyncing(ids: List<String>): Long

    /**
     * Unmarks logs (e.g. following a failed transmission) to allow them to be retried.
     */
    suspend fun unmarkAsSyncing(ids: List<String>): Long

    /**
     * Permanently evicts successfully synchronized logs from local storage.
     */
    suspend fun deleteSyncedLogs(ids: List<String>): Long

    /**
     * Clears all local logs.
     */
    suspend fun clearAllLogs()

    /**
     * Clears logs filtered by source (preserving source segregation).
     */
    suspend fun clearLogsBySource(logSource: String)

    /**
     * Prunes old logs to maintain a strict hardware storage footprint.
     */
    suspend fun pruneExcessLogs(retentionCap: Long): Long

    /**
     * Provides a reactive feed of the latest local logs for diagnostic UIs.
     */
    fun observeLatestLogs(limit: Long): Flow<List<LocalLogEntry>>

    /**
     * Returns real-time statistics about the local log queue.
     */
    fun observeStats(): Flow<LocalTelemetryStats>
}

data class LocalLogEntry(
    val id: String,
    val logSource: String,
    val actionOrEvent: String,
    val level: String,
    val workspaceId: String?,
    val branchId: String?,
    val userId: String?,
    val payload: String,
    val isSyncing: Boolean,
    val createdAt: String
)

data class LocalTelemetryStats(
    val pendingCount: Long,
    val syncingCount: Long,
    val totalCount: Long,
    val identityCount: Long,
    val appCount: Long
)
