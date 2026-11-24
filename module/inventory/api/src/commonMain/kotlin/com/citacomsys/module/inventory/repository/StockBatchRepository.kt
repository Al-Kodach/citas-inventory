package com.citacomsys.module.inventory.repository

import com.citacomsys.types.BatchId
import com.citacomsys.types.BatchItem
import com.citacomsys.types.BatchStockSnapshot
import com.citacomsys.types.StockBatch
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.Money
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

interface StockBatchRepository {

    suspend fun save(batch: StockBatch): StockBatch

    suspend fun isExist(
        batchNumber: String,
        productId: String,
        branchId: String,
        workspaceId: String
    ): Boolean

    suspend fun findById(batchId: String, workspaceId: String): StockBatch?

    suspend fun updateBatchMetadata(
        batchId: String,
        newBatchNumber: String,
        newExpiry: LocalDate?,
        workspaceId: String
    )

    suspend fun getBatchSnapshotsFIFO(
        workspaceId: String,
        productId: String,
        shouldLock: Boolean
    ): List<BatchStockSnapshot>

    suspend fun findSnapshotById(batchId: BatchId, workspaceId: String): BatchStockSnapshot?

    suspend fun delete(workspaceId: String, batchId: String): Boolean

    suspend fun getChangeList(
        since: Instant?,
        organizationId: String,
        workspaceId: String? = null,
        branchId: String? = null
    ): ChangeListResponse<BatchItem, String>

    /**
     * Retrieves all batches across a workspace that are expiring within a certain threshold.
     */
    suspend fun getExpiringBatches(
        workspaceId: String,
        thresholdDays: Int
    ): List<BatchStockSnapshot>

    suspend fun countExpiryRisks(
        workspaceId: String,
        branchId: String? = null,
        thresholdDays: Int = 90
    ): Long

    suspend fun countActiveInventory(workspaceId: String, branchId: String? = null): Long

    suspend fun findOrCreateMatchingBranchBatch(
        workspaceId: String,
        branchId: String,
        productId: String,
        batchNumber: String,
        expiryDate: LocalDate?,
        costPrice: Money,
        sellingPrice: Money
    ): String
}
