package com.citacomsys.inventory.home.server

import com.citacomsys.home.api.DashboardOverview
import com.citacomsys.api.sales.SaleRepository
import com.citacomsys.api.sales.SaleStatus
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.module.inventory.repository.StockBatchRepository
import kotlin.time.Instant

internal class DashboardService(
    private val saleRepo: SaleRepository,
    private val productRepo: ProductRepository,
    private val stockBatchRepo: StockBatchRepository
) {
    /**
     * Aggregates key operational metrics.
     */
    suspend fun getDashboardOverview(
        workspaceId: String,
        branchId: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): DashboardOverview {
        val start = startDate?.let { Instant.parse(it) }
        val end = endDate?.let { Instant.parse(it) }

        val filteredSales = saleRepo.getSales(
            workspaceId, branchId, start, end
        )

        val completedSales = filteredSales.filter { it.status == SaleStatus.COMPLETED }
        val totalRevenueMinor = completedSales.sumOf { it.totalAmount.amountMinor }

        val pendingOrdersCount = filteredSales.count { it.status == SaleStatus.PENDING }

        val lowStockCount = productRepo.countLowStock(
            workspaceId, branchId
        )
        val expiryRiskCount = stockBatchRepo.countExpiryRisks(
            workspaceId, branchId, thresholdDays = 30
        )
        val activeInventoryCount = stockBatchRepo.countActiveInventory(
            workspaceId, branchId
        )

        val topProducts = saleRepo.getTopSellingProducts(
            workspaceId, branchId, start, end
        )
        val revenueTrend = saleRepo.getRevenueTrend(
            workspaceId, branchId, start, end
        )

        return DashboardOverview(
            totalRevenueMinor = totalRevenueMinor,
            pendingOrdersCount = pendingOrdersCount.toLong(),
            branchId = branchId,
            lowStockCount = lowStockCount,
            expiryRiskCount = expiryRiskCount,
            activeInventoryCount = activeInventoryCount,
            revenueGrowthPercentage = 0.0,
            stockoutRiskCount = 0,
            topProducts = topProducts,
            revenueTrend = revenueTrend
        )
    }
}
