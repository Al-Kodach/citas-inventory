package com.citacomsys.home.data.repository

import com.citacomsys.types.TopProductMetric
import com.citacomsys.types.TrendPoint
import kotlin.time.Instant

data class DashboardSummaryData(
    val totalRevenueMinor: Long,
    val lowStockCount: Long,
    val expiryRiskCount: Long,
    val pendingOrdersCount: Long = 0,
    val activeInventoryCount: Long = 0,
    val revenueGrowthPercentage: Double = 0.0,
    val stockoutRiskCount: Long = 0,
    val topProducts: List<TopProductMetric> = emptyList(),
    val revenueTrend: List<TrendPoint> = emptyList()
)

data class DashboardActivityData(
    val id: String,
    val type: String,
    val quantity: Long,
    val createdAt: Instant,
    val productName: String,
    val unitOfMeasure: String,
    val batchNumber: String?,
    val activityType: String?,
    val actorName: String
)
