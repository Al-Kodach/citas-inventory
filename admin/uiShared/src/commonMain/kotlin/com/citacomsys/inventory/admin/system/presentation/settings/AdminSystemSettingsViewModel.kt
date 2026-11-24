package com.citacomsys.inventory.admin.system.presentation.settings

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.system.SystemApiService
import com.citacomsys.api.platform.model.SystemSettings
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing global platform configurations.
 */
class AdminSystemSettingsViewModel(
    private val api: SystemApiService,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    private val _settingsState = MutableStateFlow<UiResult<SystemSettings>>(UiResult.Idle)
    val settingsState = _settingsState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _settingsState.value = UiResult.Loading
            _settingsState.value = runCatching { api.getSettings() }.asUiResult()
        }
    }

    fun updateSettings(settings: SystemSettings) {
        viewModelScope.launch {
            api.updateSettings(settings).onSuccess {
                showSuccess("System settings updated successfully")
                loadSettings()
            }.onFailure {
                showError(it.message ?: "Failed to update settings")
            }
        }
    }
}
