package com.citacomsys.home.data.repository

import com.citacomsys.core.database.dao.DashboardDao
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.database.*
import com.citacomsys.types.TopProductMetric
import com.citacomsys.types.TrendPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

internal class DashboardRepositoryImpl(
    private val dashboardDao: DashboardDao,
    private val sessionManager: AppSessionManager,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : DashboardRepository {

    private val branchId get() = sessionManager.activeViewBranchId.value ?: ""

    override fun observeSummary(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Flow<DashboardSummaryData> {
        if (startDate == null && endDate == null) {
            return dashboardDao.observeSummary(branchId).map { it.toData() }
        }

        val cacheId = "BRANCH_${branchId}_${startDate}_${endDate}"
        return dashboardDao.observeSummaryCache(cacheId).map { it.toData(json) }
    }


    override fun observeActivityFeed(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Flow<List<DashboardActivityData>> =
        dashboardDao.observeActivityFeed(branchId)
            .map { list -> list.map { it.toData() } }

    override fun observeBusinessSummary(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Flow<DashboardSummaryData> {
        if (startDate == null && endDate == null) {
            return dashboardDao.observeBusinessSummary().map { it.toData() }
        }
        val cacheId = "BUSINESS_WIDE_${startDate}_${endDate}"
        return dashboardDao.observeSummaryCache(cacheId).map { it.toData(json) }
    }

    override fun observeBusinessActivityFeed(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Flow<List<DashboardActivityData>> =
        dashboardDao.observeBusinessActivityFeed().map { list -> list.map { it.toData() } }
}

private fun GetDashboardSummary.toData() =
    DashboardSummaryData(
        totalRevenueMinor = totalRevenueMinor,
        lowStockCount = lowStockCount,
        expiryRiskCount = expiryRiskCount,
        pendingOrdersCount = pendingOrdersCount,
        activeInventoryCount = activeInventoryCount
    )

private fun GetBusinessSummary.toData() =
    DashboardSummaryData(
        totalRevenueMinor = totalRevenueMinor,
        lowStockCount = lowStockCount,
        expiryRiskCount = expiryRiskCount,
        pendingOrdersCount = pendingOrdersCount,
        activeInventoryCount = activeInventoryCount
    )

private fun DashboardSummaryCache?.toData(json: Json) = this?.let {
    DashboardSummaryData(
        totalRevenueMinor = it.total_revenue_minor,
        lowStockCount = it.low_stock_count,
        expiryRiskCount = it.expiry_risk_count,
        pendingOrdersCount = it.pending_orders_count,
        activeInventoryCount = it.active_inventory_count,
        revenueGrowthPercentage = it.revenue_growth_percentage,
        stockoutRiskCount = it.stockout_risk_count,
        topProducts = it.top_products_json?.let { data ->
            json.decodeFromString<List<TopProductMetric>>(
                data
            )
        } ?: emptyList(),
        revenueTrend = it.revenue_trend_json?.let { data ->
            json.decodeFromString<List<TrendPoint>>(
                data
            )
        } ?: emptyList()
    )
} ?: DashboardSummaryData(0, 0, 0, 0, 0, 0.0, 0)

private fun GetDashboardActivityFeed.toData() = DashboardActivityData(
    id,
    type,
    quantity,
    created_at,
    productName,
    unitOfMeasure,
    batchNumber,
    activityType,
    actorName
)

private fun GetBusinessActivityFeed.toData() = DashboardActivityData(
    id,
    type,
    quantity,
    created_at,
    productName,
    unitOfMeasure,
    batchNumber,
    activityType,
    actorName
)
