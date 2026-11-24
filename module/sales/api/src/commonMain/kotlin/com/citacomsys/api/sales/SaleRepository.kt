package com.citacomsys.api.sales

import com.citacomsys.types.TopProductMetric
import com.citacomsys.types.TrendPoint
import kotlin.time.Instant

interface SaleRepository {

    suspend fun save(sale: Sale): Result<Unit>

    suspend fun saveItems(items: List<SaleItem>): Result<Unit>

    suspend fun savePayment(payments: List<Payment>): Result<Unit>

    suspend fun updateStatus(saleId: String, status: SaleStatus): Result<Unit>

    suspend fun getById(saleId: String): Result<Sale>

    suspend fun geSales(workspaceId: String): List<Sale>

    suspend fun getSales(
        workspaceId: String,
        branchId: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): List<Sale>

    suspend fun getSaleDetails(saleId: String, workspaceId: String): SaleDetails
    suspend fun existsById(saleId: String): Boolean
    suspend fun getSaleRecord(saleId: String, workspaceId: String): SalesRecord?

    suspend fun getTopSellingProducts(
        workspaceId: String,
        branchId: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 10
    ): List<TopProductMetric>

    suspend fun getRevenueTrend(
        workspaceId: String,
        branchId: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): List<TrendPoint>
}
