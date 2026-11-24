package com.citacomsys.inventory.admin.app

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

enum class AdminAppStage {
    INITIALIZING,
    AUTHENTICATION,
    MAIN_APP
}

class AdminAppStageOrchestrator(
    private val tokenStore: TokenStore,
    private val sessionManager: AppSessionManager,
    private val scope: CoroutineScope
) {
    private val TAG = "ADMIN_STAGE_ORCHESTRATOR"

    private val _currentStage = MutableStateFlow(AdminAppStage.INITIALIZING)
    val currentStage: StateFlow<AdminAppStage> = _currentStage.asStateFlow()
    private var isCoreReady = false

    init {
        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.CoreReady>().collect {
                isCoreReady = true
                evaluate()
            }
        }

        combine(
            AdminAppPlatform.lifecycleState,
        ) { lifecycle ->
            if (lifecycle == AppLifecycleEvent.Bootstrapping) {
                setStage(AdminAppStage.INITIALIZING)
            }
        }.launchIn(scope)

        combine(
            tokenStore.accessToken,
            sessionManager.appUser
        ) { token, user ->
            evaluate()
        }.launchIn(scope)
    }

    fun evaluate() {
        scope.launch {
            if (isCoreReady.not()) return@launch

            val token = tokenStore.getAccessToken()
            val user = sessionManager.getUser()
            val hasSession = !token.isNullOrEmpty() && user != null

            if (hasSession) {
                Napier.i(tag = TAG) { "Admin session active. Launching Admin Dashboard." }
                _currentStage.value = AdminAppStage.MAIN_APP
            } else {
                Napier.i(tag = TAG) { "No admin session. Requesting authentication." }
                _currentStage.value = AdminAppStage.AUTHENTICATION
            }
        }
    }

    fun setStage(stage: AdminAppStage) {
        _currentStage.value = stage
    }
}
