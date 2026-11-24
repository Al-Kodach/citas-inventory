package com.citacomsys.core.database.dao.inventory

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.database.BatchSnapshotEntity
import com.citacomsys.database.StockBatchEntity
import com.citacomsys.types.BatchItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface StockBatchDao {

    fun getAll(productId: String): Flow<List<BatchSnapshotEntity>>
    suspend fun getItem(id: String): BatchSnapshotEntity?
    fun observeItem(id: String): Flow<BatchSnapshotEntity?>
    fun search(query: String): Flow<List<BatchSnapshotEntity>>
    suspend fun findByBatchNumber(batchNumber: String, productId: String): BatchSnapshotEntity?


    // Metadata actions
    suspend fun save(entity: StockBatchEntity)
    suspend fun getBatchMeta(id: String): StockBatchEntity?
    fun observeBatch(batchId: String): Flow<StockBatchEntity?>

    suspend fun updateBatchMetadata(
        id: String,
        batchNumber: String,
        expiryDate: LocalDate?,
        updatedAt: Instant
    )

    suspend fun getMaxVersionTimestamp(): Instant? = null

    companion object {
        fun mapToItem(entity: BatchSnapshotEntity): BatchItem {
            return BatchItem(
                batchId = entity.batch_id,
                batchNumber = entity.batch_number,
                productId = entity.product_id,
                workspaceId = entity.workspace_id,
                branchId = entity.batch_id,
                availableQuantity = entity.available_quantity.toInt(),
                sellingPriceAmountMinor = entity.selling_price_amount_minor,
                sellingPriceCurrency = entity.selling_price_currency,
                expiryDate = entity.expiry_date,
                versionTimestamp = entity.version_timestamp,
            )
        }
    }
}

internal class SqlDelightStockBatchDao(
    private val manager: InventoryDatabaseManager,
    private val sessionManager: AppSessionManager
) : StockBatchDao {

    override fun observeItem(id: String): Flow<BatchSnapshotEntity?> {
        return manager.observeSingle { it.batchSnapshotQueries.selectById(id) }
    }

    override suspend fun getItem(id: String): BatchSnapshotEntity? {
        return manager.getDatabase().batchSnapshotQueries.selectById(id).executeAsOneOrNull()
    }

    override suspend fun save(entity: StockBatchEntity) {
        val db = manager.getDatabase()
        db.transaction {
            db.stockBatchQueries.insertOrReplace(entity)
        }
    }

    override fun getAll(productId: String): Flow<List<BatchSnapshotEntity>> =
        manager.database.flatMapLatest { db ->
            db.batchSnapshotQueries.selectByProductId(productId).asFlow()
                .mapToList(manager.dispatcher)
        }

    override fun search(query: String): Flow<List<BatchSnapshotEntity>> {
        return manager.observesList {
            it.batchSnapshotQueries.search(
                workspaceId = sessionManager.activeWorkspaceIdFlow.value,
                branchId = sessionManager.activeViewBranchId.value,
                query = query
            )
        }
    }

    override suspend fun getMaxVersionTimestamp(): Instant? = null

    override suspend fun getBatchMeta(id: String): StockBatchEntity? {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.stockBatchQueries.selectById(id).executeAsOneOrNull()
        }
    }

    override suspend fun findByBatchNumber(
        batchNumber: String,
        productId: String
    ): BatchSnapshotEntity? {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.batchSnapshotQueries.selectByNumber(number = batchNumber, pId = productId)
                .executeAsOneOrNull()
        }
    }

    override suspend fun updateBatchMetadata(
        id: String,
        batchNumber: String,
        expiryDate: LocalDate?,
        updatedAt: Instant
    ) {
        val db = manager.getDatabase()

        db.transaction {
            db.stockBatchQueries.updateBatchMetadata(
                batch_number = batchNumber,
                expiry_date_ms = expiryDate?.toEpochDays(),
                expiry_date = expiryDate?.toString(),
                id = id,
                updated_at = updatedAt
            )
        }
    }

    override fun observeBatch(batchId: String): Flow<StockBatchEntity?> {
        return manager.observeSingle { it.stockBatchQueries.selectById(batchId) }
    }
}
