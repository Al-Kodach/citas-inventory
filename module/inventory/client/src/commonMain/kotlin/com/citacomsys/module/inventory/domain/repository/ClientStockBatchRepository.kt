package com.citacomsys.module.inventory.domain.repository

import com.citacomsys.module.inventory.request.AdjustStockRequest
import com.citacomsys.module.inventory.request.PriceChangeRequest
import com.citacomsys.module.inventory.request.RestockRequest
import com.citacomsys.types.BatchItem
import com.citacomsys.types.StockBatch
import com.citacomsys.types.StockMovement
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ClientStockBatchRepository {
    fun search(query: String): Flow<List<BatchItem>>
    fun observeBatchesForProduct(productId: String): Flow<List<BatchItem>>
    fun observeBatch(batchId: String): Flow<BatchItem?>
    fun observeBatchView(batchId: String): Flow<StockBatch?>
    fun observeStockMoventByProduct(productId: String): Flow<List<StockMovement>>
    fun observeMovementsForBatch(batchId: String): Flow<List<StockMovement>>
    suspend fun findById(id: String): StockBatch?
    suspend fun syncMovementsFromCloud(productId: String, batchId: String? = null): Result<Unit>
    suspend fun fetchBatchDetailsCloud(batchId: String): Result<StockBatch>

    /**
     * Commits a restock to local DB and enqueues for cloud sync if subscribed.
     */
    suspend fun restock(
        request: RestockRequest,
        actorId: String,
        actorName: String
    ): Result<Unit>

    /**
     * Commits an adjustment to local DB and enqueues for cloud sync if subscribed.
     */
    suspend fun adjustStock(
        request: AdjustStockRequest,
        actorId: String,
        actorName: String
    ): Result<Unit>

    suspend fun updateBatchMetadata(
        batchId: String,
        batchNumber: String,
        productId: String,
        expiryDate: LocalDate?,
        actorId: String,
        actorName: String
    ): Result<Unit>

    /**
     * Updates the cost or selling basePrice of a specific stock batch.
     */
    suspend fun updatePrice(
        request: PriceChangeRequest,
        actorId: String,
        actorName: String
    ): Result<Unit>

    /**
     * Triggers a pull-sync from the cloud to updateMetadata local inventory states.
     */
    suspend fun onRefresh(): Result<Unit>

    suspend fun delete(
        id: String,
        actorId: String,
        actorName: String
    )
}
