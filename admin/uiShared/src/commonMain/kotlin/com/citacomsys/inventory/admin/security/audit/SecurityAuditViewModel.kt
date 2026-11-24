package com.citacomsys.inventory.admin.security.audit

import androidx.lifecycle.viewModelScope
import com.citacomsys.feature.api.telemetry.SiemApiService
import com.citacomsys.feature.api.telemetry.TraceTimelineEntry
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecurityAuditViewModel(
    private val siemApi: SiemApiService,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    private val _auditFeed = MutableStateFlow<UiResult<List<TraceTimelineEntry>>>(UiResult.Idle)
    val auditFeed = _auditFeed.asStateFlow()

    private val _traceDetails = MutableStateFlow<UiResult<List<TraceTimelineEntry>>>(UiResult.Idle)
    val traceDetails = _traceDetails.asStateFlow()

    fun loadGlobalFeed() {
        viewModelScope.launch {
            _auditFeed.value = UiResult.Loading
            _auditFeed.value = runCatching { siemApi.getGlobalSecurityFeed() }.asUiResult()
        }
    }

    fun loadRequestTrace(requestId: String) {
        viewModelScope.launch {
            _traceDetails.value = UiResult.Loading
            _traceDetails.value = runCatching { siemApi.getRequestTrace(requestId) }.asUiResult()
        }
    }
}