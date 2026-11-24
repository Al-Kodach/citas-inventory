package com.citacomsys.app.shared.sync

import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getOrDefault
import com.citacomsys.core.ui.result.getOrEmpty
import com.citacomsys.database.PendingRequestEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class SyncManagerViewModel(
    notification: InAppNotificationService,
    private val syncEngine: EntitySyncEngine,
    private val dbManager: InventoryDatabaseManager,
    private val syncPrefs: SyncPreferencesRepository,
) : BaseViewModel(notification) {

    // Live status derivations from the core Sync Engine hooks
    val isSyncing = syncEngine.isSyncing
    val activeSyncCount = syncEngine.activeSyncCount

    // Last sync metadata stream (push/pull timestamps, errors)
    val lastSyncMetadata: StateFlow<LastSyncMetadata?> =
        syncPrefs.observeLastSyncMetadata()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    // State-driven streams observing the landing ledger tables directly
    val pendingRequests: StateFlow<UiResult<List<PendingRequestEntity>>> =
        dbManager.database.flatMapLatest { db ->
            db.pendingRequestQueries.observeAll()
                .asFlow()
                .mapToList(viewModelScope.coroutineContext)
        }.asUiResult()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                UiResult.Loading
            )

    // Computed aggregates for metrics bar panel matrix
    val syncMetrics: StateFlow<SyncMetricsData> = pendingRequests.map { state ->
        val items = state.getOrEmpty()
        SyncMetricsData(
            pendingCount = items.count { it.canRetry && !it.isSyncing },
            syncingCount = items.count { it.isSyncing },
            deadLetterCount = items.count { !it.canRetry }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SyncMetricsData()
    )

    /**
     * Instantly triggers a complete system sync cycle.
     */
    fun forceSyncAll() {
        viewModelScope.launch {
            try {
                syncEngine.syncAll()
                showSuccess("Sync request submitted successfully.")
            } catch (e: Exception) {
                showError("Failed to issue global sync trigger command: ${e.message}")
            }
        }
    }

    /**
     * Allows managers to revive a failed item from the dead-letter log.
     */
    fun retryRequest(id: String) {
        viewModelScope.launch {
            dbManager.getDatabase().pendingRequestQueries.unmarkAsSyncing(
                id = id,
                reason = "Manual resetWorkspaceSession requested by administrator.",
                timestamp = Clock.System.now()
            )
            showSuccess("Request returned to active sync pipeline.")
        }
    }

    /**
     * Resets all dead-letter requests for retry.
     */
    fun retryAllDeadLetters() {
        viewModelScope.launch {
            val deadLetters = pendingRequests.value.getOrEmpty().filter { !it.canRetry }
            if (deadLetters.isEmpty()) return@launch

            val queries = dbManager.getDatabase().pendingRequestQueries
            val now = Clock.System.now()
            queries.transaction {
                deadLetters.forEach { item ->
                    queries.unmarkAsSyncing(
                        id = item.id,
                        reason = "Batch resetWorkspaceSession by administrator.",
                        timestamp = now
                    )
                }
            }
            showSuccess("Reset ${deadLetters.size} dead-letter requests for retry.")
        }
    }

    /**
     * Clears all dead-letter requests permanently from the queue.
     */
    fun clearAllDeadLetters() {
        viewModelScope.launch {
            val deadLetters = pendingRequests.value.getOrEmpty().filter { !it.canRetry }
            if (deadLetters.isEmpty()) return@launch

            val queries = dbManager.getDatabase().pendingRequestQueries
            queries.transaction {
                deadLetters.forEach { item ->
                    queries.deleteById(item.id)
                }
            }
            showSuccess("Cleared ${deadLetters.size} dead-letter requests from ledger.")
        }
    }

    /**
     * Erases a poison payload from the offline queue completely.
     */
    fun deleteRequest(id: String) {
        viewModelScope.launch {
            dbManager.getDatabase().pendingRequestQueries.deleteById(id)
            showSuccess("Offline item request dropped permanently.")
        }
    }

    /**
     * Generates a plain-text sync diagnostics report for copying or sharing.
     */
    fun generateDiagnosticsReport(): String {
        val meta = lastSyncMetadata.value
        val metrics = syncMetrics.value
        val total = pendingRequests.value.getOrEmpty().size

        return buildString {
            appendLine("=== CITAS PLATFORM SYNC DIAGNOSTICS REPORT ===")
            appendLine("Generated At: ${Clock.System.now()}")
            appendLine("Total Queue Entries: $total")
            appendLine("  • Pending Uploads: ${metrics.pendingCount}")
            appendLine("  • Processing: ${metrics.syncingCount}")
            appendLine("  • Dead-Letter Queue: ${metrics.deadLetterCount}")
            appendLine("Last Push Timestamp: ${meta?.lastSuccessfulPushTimestamp ?: "None"}")
            appendLine("Last Pull Timestamp: ${meta?.lastSuccessfulPullTimestamp ?: "None"}")
            meta?.lastSystemErrorMessage?.let {
                appendLine("Last System Error: $it")
            }
            appendLine("============================================")
        }
    }
}

data class SyncMetricsData(
    val pendingCount: Int = 0,
    val syncingCount: Int = 0,
    val deadLetterCount: Int = 0
)
