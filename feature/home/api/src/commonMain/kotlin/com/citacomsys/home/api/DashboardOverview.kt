package com.citacomsys.home.api

import com.citacomsys.types.TopProductMetric
import com.citacomsys.types.TrendPoint
import kotlinx.serialization.Serializable

@Serializable
data class DashboardOverview(
    val totalRevenueMinor: Long = 0,
    val lowStockCount: Long = 0,
    val expiryRiskCount: Long = 0,
    val pendingOrdersCount: Long = 0,
    val activeInventoryCount: Long = 0,
    val revenueGrowthPercentage: Double = 0.0,
    val stockoutRiskCount: Long = 0,
    val branchId: String? = null,
    val topProducts: List<TopProductMetric> = emptyList(),
    val revenueTrend: List<TrendPoint> = emptyList()
)
