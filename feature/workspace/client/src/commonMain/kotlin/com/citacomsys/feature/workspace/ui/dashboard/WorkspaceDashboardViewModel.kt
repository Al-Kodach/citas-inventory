package com.citacomsys.feature.workspace.ui.dashboard

import androidx.lifecycle.viewModelScope
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.home.data.repository.DashboardActivityData
import com.citacomsys.home.data.repository.DashboardRepository
import com.citacomsys.home.data.repository.DashboardSummaryData
import com.citacomsys.core.spi.GlobalNavigationCoordinator
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.types.EntityType
import com.citacomsys.types.PackageTier
import com.citacomsys.core.spi.PluginDashboardWidget
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceDashboardViewModel(
    private val repository: DashboardRepository,
    private val sessionManager: AppSessionManager,
    private val runtimeService: PlatformClientService,
    private val capabilityManager: UserCapabilityManager,
    private val coordinator: GlobalNavigationCoordinator,
    private val syncEngine: EntitySyncEngine,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    val activeWidgets: StateFlow<List<PluginDashboardWidget>> = coordinator.activeDashboardWidgets
    private val activeViewBranchId = sessionManager.activeViewBranchId

    val isAdvancedReportingEnabled = combine(
        runtimeService.activeWorkspaceConfig,
        capabilityManager.capabilities
    ) { config, caps ->
        val tier = config?.resource?.tier
        val isLicensed = tier == PackageTier.PRO || tier == PackageTier.ENTERPRISE
        isLicensed
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        false
    )

    private val _isBusinessMode = MutableStateFlow(false)
    val isBusinessMode = _isBusinessMode.asStateFlow()

    private val _startDate = MutableStateFlow<LocalDate?>(null)
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<LocalDate?>(null)
    val endDate = _endDate.asStateFlow()

    init {
        // Listen for real-time changes to refresh dashboard metrics
        EventBus.subscribe<EntityChangeEvent>()
            .onEach { event ->
                val activeWs = sessionManager.activeWorkspaceId
                if (activeWs != null && (event.id == activeWs || event.workspaceId == activeWs)) {
                    val activeBranch = sessionManager.activeViewBranchId.value
                    if (activeBranch == null || event.branchId == null || event.branchId == activeBranch) {
                        if (event.entityType in setOf("SALE", "STOCK_BATCH", "PRODUCT", "INVENTORY")) {
                            viewModelScope.launch {
                                syncEngine.requestSync(EntityType.DASHBOARD)
                            }
                        }
                    }
                }
            }.launchIn(viewModelScope)

        viewModelScope.launch {
            sessionManager.activeViewBranchId.collect {
                _isBusinessMode.value = it == null
            }
        }
    }

    val summary: StateFlow<UiResult<DashboardSummaryData>> = combine(
        activeViewBranchId,
        _isBusinessMode,
        _startDate,
        _endDate
    ) { branchId, isBusiness, start, end ->
        if (isBusiness) {
            repository.observeBusinessSummary(start, end)
        } else {
            repository.observeSummary(start, end)
        }
    }.flatMapLatest { it }
        .asUiResult()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            UiResult.Loading
        )

    val activityFeed: StateFlow<UiResult<List<DashboardActivityData>>> = combine(
        activeViewBranchId,
        _isBusinessMode,
        _startDate,
        _endDate
    ) { branchId, isBusiness, start, end ->
        if (isBusiness) {
            repository.observeBusinessActivityFeed(start, end)
        } else {
            repository.observeActivityFeed(start, end)
        }
    }.flatMapLatest { it }
        .asUiResult()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            UiResult.Loading
        )


    fun setDateRange(start: LocalDate?, end: LocalDate?) {
        _startDate.value = start
        _endDate.value = end
    }
}
