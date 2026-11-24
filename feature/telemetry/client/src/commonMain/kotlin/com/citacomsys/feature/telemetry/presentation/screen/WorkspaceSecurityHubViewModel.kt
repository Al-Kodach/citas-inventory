package com.citacomsys.feature.telemetry.presentation.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.feature.api.telemetry.WorkspaceSecurityApiService
import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkspaceSecurityHubViewModel(
    private val api: WorkspaceSecurityApiService,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    private val _auditFeed = MutableStateFlow<UiResult<List<AuditTimelineEntry>>>(UiResult.Idle)
    val auditFeed = _auditFeed.asStateFlow()

    init {
        loadAuditFeed()
    }

    fun loadAuditFeed(branchId: String? = null) {
        viewModelScope.launch {
            _auditFeed.value = UiResult.Loading
            _auditFeed.value = runCatching { api.getAuditFeed(branchId, 20) }.asUiResult()
        }
    }
}
