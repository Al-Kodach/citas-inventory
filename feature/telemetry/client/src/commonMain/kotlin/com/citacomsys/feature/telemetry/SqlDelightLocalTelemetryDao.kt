package com.citacomsys.feature.telemetry

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.citacomsys.core.database.DriverFactory
import com.citacomsys.core.database.createTelemetryDatabase
import com.citacomsys.feature.api.telemetry.dao.LocalLogEntry
import com.citacomsys.feature.api.telemetry.dao.LocalTelemetryDao
import com.citacomsys.feature.api.telemetry.dao.LocalTelemetryStats
import com.citacomsys.telemetry.database.TelemetryDatabase
import com.citacomsys.telemetry.database.ClientLocalAuditLog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class TelemetryDatabaseManager(
    private val driverFactory: DriverFactory
) {
    private val _database = MutableStateFlow<TelemetryDatabase?>(null)
    private val mutex = Mutex()

    suspend fun getDatabase(): TelemetryDatabase {
        return _database.value ?: mutex.withLock {
            _database.value ?: run {
                Napier.d { "Lazy Initializing TelemetryDatabase..." }
                createTelemetryDatabase(driverFactory).also { 
                    _database.value = it 
                    Napier.i { "TelemetryDatabase initialized successfully." }
                }
            }
        }
    }

    val database: Flow<TelemetryDatabase> = _database
        .onStart { getDatabase() }
        .filterNotNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class SqlDelightLocalTelemetryDao(
    private val manager: TelemetryDatabaseManager,
    private val dispatcher: CoroutineDispatcher
) : LocalTelemetryDao {

    override suspend fun insertLog(
        id: String,
        logSource: String,
        actionOrEvent: String,
        level: String,
        workspaceId: String?,
        branchId: String?,
        userId: String?,
        payload: String,
        createdAt: String
    ): Unit = withContext(dispatcher) {
        manager.getDatabase().clientLocalAuditLogQueries.insertLog(
            id,
            logSource,
            actionOrEvent,
            level,
            workspaceId ?: "",
            branchId,
            userId,
            payload,
            createdAt
        )
    }

    override suspend fun getLogsForSync(
        allowedSources: List<String>,
        timeCutoff: String,
        limit: Long
    ): List<LocalLogEntry> = withContext(dispatcher) {
        manager.getDatabase().clientLocalAuditLogQueries
            .getLogsForSync(allowedSources, timeCutoff, limit)
            .executeAsList()
            .map { it.toEntry() }
    }

    override suspend fun markAsSyncing(ids: List<String>): Long = withContext(dispatcher) {
        manager.getDatabase().clientLocalAuditLogQueries.markAsSyncing(ids)
        ids.size.toLong()
    }

    override suspend fun unmarkAsSyncing(ids: List<String>): Long = withContext(dispatcher) {
        manager.getDatabase().clientLocalAuditLogQueries.unmarkAsSyncing(ids)
        ids.size.toLong()
    }

    override suspend fun deleteSyncedLogs(ids: List<String>): Long = withContext(dispatcher) {
        manager.getDatabase().clientLocalAuditLogQueries.deleteSyncedLogs(ids)
        ids.size.toLong()
    }

    override suspend fun clearAllLogs() = withContext(dispatcher) {
        val db = manager.getDatabase()
        db.transaction {
            db.clientLocalAuditLogQueries.deleteAllLogs()
        }
    }

    override suspend fun clearLogsBySource(logSource: String) = withContext(dispatcher) {
        val db = manager.getDatabase()
        db.transaction {
            db.clientLocalAuditLogQueries.deleteLogsBySource(logSource)
        }
    }

    override suspend fun pruneExcessLogs(retentionCap: Long): Long = withContext(dispatcher) {
        manager.getDatabase().clientLocalAuditLogQueries.pruneExcessLogs(retentionCap)
        retentionCap // Returning cap as a placeholder
    }

    override fun observeLatestLogs(limit: Long): Flow<List<LocalLogEntry>> {
        return manager.database.flatMapLatest { db ->
            db.clientLocalAuditLogQueries.getLatestLocalLogs(limit).asFlow().mapToList(dispatcher).map { list ->
                list.map { it.toEntry() }
            }
        }
    }

    override fun observeStats(): Flow<LocalTelemetryStats> {
        return observeLatestLogs(100L).map { items ->
            LocalTelemetryStats(
                pendingCount = items.count { !it.isSyncing }.toLong(),
                syncingCount = items.count { it.isSyncing }.toLong(),
                totalCount = items.size.toLong(),
                identityCount = items.count { it.logSource == "IDENTITY" }.toLong(),
                appCount = items.count { it.logSource == "APPLICATION" }.toLong()
            )
        }
    }

    private fun ClientLocalAuditLog.toEntry() = LocalLogEntry(
        id = id,
        logSource = log_source,
        actionOrEvent = action_or_event,
        level = level,
        workspaceId = workspace_id,
        branchId = branch_id,
        userId = user_id,
        payload = payload,
        isSyncing = is_syncing,
        createdAt = created_at
    )
}
