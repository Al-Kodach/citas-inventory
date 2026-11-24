package com.citacomsys.feature.telemetry.presentation.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.feature.api.telemetry.dao.LocalLogEntry
import com.citacomsys.feature.api.telemetry.dao.LocalTelemetryDao
import com.citacomsys.feature.api.telemetry.dao.LocalTelemetryStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class LocalTelemetryViewModel(
    notification: InAppNotificationService,
    private val telemetryClient: TelemetryLogClient,
    private val localDao: LocalTelemetryDao,
    private val sessionManager: AppSessionManager
) : BaseViewModel(notification) {

    val localLogs: StateFlow<List<LocalLogEntry>> = localDao.observeLatestLogs(limit = 100L)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val logStats: StateFlow<LocalTelemetryStats> = localDao.observeStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            LocalTelemetryStats(0, 0, 0, 0, 0)
        )

    private val _isFlushing = MutableStateFlow(false)
    val isFlushing = _isFlushing.asStateFlow()

    init {
        // Enforce structural hardware cache pruning limits on database startup
        viewModelScope.launch(Dispatchers.Default) {
            localDao.pruneExcessLogs(retentionCap = 5000L)
        }
    }


    /**
     * Triggers a manual time-sliced chunk upload over Ktor network client.
     */
    fun forceSyncLogsNow() {
        if (sessionManager.isSubscribed()) {
            showError("Standalone clients maintain tracking data on physical disk to assist debugging, but network uploads are disabled.")
            return
        }

        viewModelScope.launch {
            _isFlushing.value = true
            val result = telemetryClient.syncLogsToServer(
                targetSources = emptySet(),
                maxAgeWindow = Clock.System.now(),
                chunkLimit = 100L
            )

            result.onSuccess { success ->
                if (success) showSuccess("Telemetry metrics synchronized safely.")
                else showError("Cloud database cluster temporarily unreachable.")
                _isFlushing.value = false
            }.onFailure { error ->
                showError("Sync failure: ${error.message}")
                _isFlushing.value = false
            }
        }
    }
}
