package com.citacomsys.core.database.dao

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.database.GetDashboardActivityFeed
import com.citacomsys.database.GetDashboardSummary
import com.citacomsys.database.GetBusinessActivityFeed
import com.citacomsys.database.GetBusinessSummary
import com.citacomsys.database.DashboardSummaryCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface DashboardDao {
    fun observeSummary(branchId: String): Flow<GetDashboardSummary>
    fun observeActivityFeed(branchId: String): Flow<List<GetDashboardActivityFeed>>

    fun observeBusinessSummary(): Flow<GetBusinessSummary>
    fun observeBusinessActivityFeed(): Flow<List<GetBusinessActivityFeed>>

    fun observeSummaryCache(id: String): Flow<DashboardSummaryCache?>
}

internal class SqlDelightDashboardDao(
    private val manager: InventoryDatabaseManager,
    sessionManager: AppSessionManager
) : DashboardDao {

    private val workspaceId = sessionManager.activeWorkspaceId ?: ""

    override fun observeSummary(branchId: String): Flow<GetDashboardSummary> {
        return manager.observeSingle { db ->
            db.dashboardQueries.getDashboardSummary(branchId, workspaceId) }
            .map { it!! }
    }

    override fun observeActivityFeed(branchId: String): Flow<List<GetDashboardActivityFeed>> {
        return manager.observesList { db -> db.dashboardQueries.getDashboardActivityFeed(branchId, workspaceId) }
    }

    override fun observeBusinessSummary(): Flow<GetBusinessSummary> {
        return manager.observeSingle { db -> db.dashboardQueries.getBusinessSummary(workspaceId) }
            .map { it!! }
    }

    override fun observeBusinessActivityFeed(): Flow<List<GetBusinessActivityFeed>> {
        return manager.observesList { db -> db.dashboardQueries.getBusinessActivityFeed(workspaceId) }
    }

    override fun observeSummaryCache(id: String): Flow<DashboardSummaryCache?> {
        return manager.observeSingle { db -> db.dashboardQueries.getSummaryCache(id, workspaceId) }
    }
}
