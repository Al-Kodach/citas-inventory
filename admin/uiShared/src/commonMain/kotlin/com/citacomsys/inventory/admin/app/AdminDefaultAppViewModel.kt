package com.citacomsys.inventory.admin.app

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.shared.event.DeviceFingerprintMismatch
import com.citacomsys.shared.event.DeviceLocked
import com.citacomsys.shared.event.DeviceRevoked
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SecurityViolationEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

open class AdminDefaultAppViewModel(
    notificationService: InAppNotificationService,
    private val prefs: PreferencesRepository
) : BaseViewModel(notificationService) {
    private val _updateCheckState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val updateCheckState = _updateCheckState.asStateFlow()

    init {
        // Observe security violations and lock the terminal if critical
        viewModelScope.launch {
            EventBus.subscribe<SecurityViolationEvent>().collect {
                // If it's a critical violation (Rooted or Debugger in production), lock the app
                if (it.violation == "RootedDevice" || it.violation == "DebuggerAttached") {
                    setLocked(true)
                }
            }
        }

        combine(
            EventBus.subscribe<DeviceFingerprintMismatch>(),
            EventBus.subscribe<DeviceRevoked>(),
            EventBus.subscribe<DeviceLocked>(),
        ) { _, _, _ ->
            // wipe out system
        }.launchIn(viewModelScope)
    }

    fun setLocked(locked: Boolean) {
    }
}
