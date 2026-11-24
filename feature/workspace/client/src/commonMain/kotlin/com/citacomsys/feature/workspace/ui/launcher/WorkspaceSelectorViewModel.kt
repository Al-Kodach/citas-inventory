package com.citacomsys.feature.workspace.ui.launcher

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.kernel.service.PlatformRuntimeStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WorkspaceSelectorViewModel(
    platformStore: PlatformRuntimeStore,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    val workspaces: StateFlow<List<WorkspaceSummary>> = platformStore.organizationConfig
        .map { it?.workspaces ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
