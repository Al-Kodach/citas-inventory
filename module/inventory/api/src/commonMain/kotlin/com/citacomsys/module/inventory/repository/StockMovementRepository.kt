package com.citacomsys.module.inventory.repository

import com.citacomsys.types.ProductInventoryState
import com.citacomsys.types.StockMovement
import kotlin.time.Instant

interface StockMovementRepository {

    suspend fun create(movement: StockMovement)

    suspend fun saveAll(movements: List<StockMovement>)

    suspend fun getStockMovementsForProduct(
        productId: String,
        batchId: String,
        since: Instant?,
        workspaceId: String,
        branchId: String? = null,
    ): List<StockMovement>

    suspend fun findDirtyProductIds(since: Instant, workspaceId: String): Set<String>

    suspend fun getProductsState(workspaceId: String, productIds: List<String>): List<ProductInventoryState>
}
