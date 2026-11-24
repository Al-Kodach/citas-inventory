package com.citacomsys.feature.organization.ui.monitoring

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.feature.api.telemetry.OrganizationSecurityApiService
import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrganizationMonitoringViewModel(
    private val api: OrganizationSecurityApiService,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    private val _auditFeed = MutableStateFlow<UiResult<List<AuditTimelineEntry>>>(UiResult.Idle)
    val auditFeed = _auditFeed.asStateFlow()

    init {
        loadAuditFeed()
    }

    fun loadAuditFeed() {
        viewModelScope.launch {
            _auditFeed.value = UiResult.Loading
            _auditFeed.value = runCatching { api.getAuditFeed(50) }.asUiResult()
        }
    }
}
