package com.citacomsys.inventory.admin.app.dashboard.presentation

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.dashboard.AdminDashboardApiService
import com.citacomsys.admin.api.dashboard.AdminDashboardOverview
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminDashboardViewModel(
    private val api: AdminDashboardApiService,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    private val _summary = MutableStateFlow<UiResult<AdminDashboardOverview>>(UiResult.Idle)
    val summary = _summary.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _summary.value = UiResult.Loading
            _summary.value = runCatching { api.getPlatformSummary() }.asUiResult()
        }
    }
}
