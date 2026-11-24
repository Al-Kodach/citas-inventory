package com.citacomsys.app.shared.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.app.shared.app.AppStage
import com.citacomsys.app.shared.app.AppStageOrchestrator
import com.citacomsys.app.shared.boot.AppPlatform
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.spi.AppOperatingMode
import com.citacomsys.core.spi.NavigationState
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import kotlinx.coroutines.launch

class AppViewModel(
    private val prefs: PreferencesRepository,
    sessionManager: AppSessionManager,
    private val orchestrator: AppStageOrchestrator
) : ViewModel() {

    val currentStage = orchestrator.currentStage
    val workStationInfo = sessionManager.workStationInfo
    val activeWorkspaceId = sessionManager.activeWorkspaceIdFlow
    val isDarkTheme = prefs.isDarkTheme
    val userFontScale = prefs.userFontScale

    val lifecycleState = AppPlatform.lifecycleState

    fun toggleTheme() {
        prefs.toggleTheme()
    }

    fun nextFontScale() {
        prefs.nextFontScale()
    }

    fun setStage(stage: AppStage) {
        orchestrator.setStage(stage)
    }

    fun workspaceConfigurationCompleted() {
        orchestrator.workspaceConfigurationCompleted()
    }

    fun recordNavState(state: NavigationState, mode: AppOperatingMode) {
        orchestrator.recordNavState(state, mode)
    }

    fun switchAppMode() {
        orchestrator.switchAppMode()
    }

    fun evaluate() {
        orchestrator.evaluate()
    }

    val workspaceSelectEvent get() = orchestrator.workspaceSelectEvent

    fun logout() {
        viewModelScope.launch {
            EventBus.publish(AppLifecycleEvent.UserSessionRevoked)
        }
    }
}
