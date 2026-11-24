package com.citacomsys.home.sync

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.database.DashboardSummaryCache
import com.citacomsys.home.api.DashboardApi
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.event.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.Koin
import kotlin.time.Clock

class BusinessDashboardSyncHandler(
    private val dashboardApi: DashboardApi,
    private val dbManager: InventoryDatabaseManager,
    private val sessionManager: AppSessionManager,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val clock: Clock = Clock.System,
): StartupTask {
    override val id: String = "workspace.dashboard"
    override val runLevel: StartupRunLevel = StartupRunLevel.WORKSPACE_READY

    override suspend fun run(koin: Koin) {
        scope.launch {
            combine(
                sessionManager.activeWorkspaceIdFlow,
                EventBus.subscribe<AppLifecycleEvent.WorkspaceActive>()
            ) { wsId, _ ->
                if (wsId != null) {
                    refresh()
                }
            }.launchIn(scope)

            EventBus.subscribe<EntityChangeEvent>().onEach { event ->
                val activeWs = sessionManager.activeWorkspaceId
                if (activeWs != null && (event.id == activeWs || event.workspaceId == activeWs)) {
                    if (event.entityType in setOf("SALE", "STOCK_BATCH", "PRODUCT", "BILLING")) {
                        refresh()
                    }
                }
            }.launchIn(scope)
        }
    }

    suspend fun refresh(): Result<Unit> {
        return runCatching {
            val branchId = sessionManager.writeTargetBranchId.value
            // Pull from server
            val result = dashboardApi.getDashboardOverview(branchId = branchId).getOrThrow()

            // Update local cache
            val cacheId = if (branchId != null) "BRANCH_$branchId" else "BUSINESS_WIDE"

            dbManager.transaction {
                dbManager.getDatabase().dashboardQueries.insertSummaryCache(
                    DashboardSummaryCache(
                        id = cacheId,
                        branch_id = branchId,
                        workspace_id = sessionManager.activeWorkspaceId ?: "",
                        total_revenue_minor = result.totalRevenueMinor,
                        low_stock_count = result.lowStockCount,
                        expiry_risk_count = result.expiryRiskCount,
                        pending_orders_count = result.pendingOrdersCount,
                        active_inventory_count = result.activeInventoryCount,
                        revenue_growth_percentage = result.revenueGrowthPercentage,
                        stockout_risk_count = result.stockoutRiskCount,
                        top_products_json = json.encodeToString(result.topProducts),
                        revenue_trend_json = json.encodeToString(result.revenueTrend),
                        cached_at = clock.now()
                    )
                )
            }
        }
    }
}
