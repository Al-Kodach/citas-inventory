package com.citacomsys.core.database.dao.inventory

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.dao.BaseDao
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.database.StockMovementEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface StockMovementDao : BaseDao<StockMovementEntity, StockMovementEntity, String> {
    suspend fun save(entities: List<StockMovementEntity>)
    fun observeByBatch(batchId: String): Flow<List<StockMovementEntity>>
    fun observeByProduct(productId: String, branchId: String?): Flow<List<StockMovementEntity>>
    suspend fun getMaxCreatedAtForProduct(productId: String, branchId: String): Instant?
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
internal class SqlDelightStockMovementDao(
    private val manager: InventoryDatabaseManager,
    private val quotaGuard: WorkspaceResourceQuotaGuard
) : StockMovementDao {

    override suspend fun save(entities: List<StockMovementEntity>) {
        val db = manager.getDatabase()
        db.transaction {
            entities.forEach { e ->
                db.stockMovementQueries.insert(e)
            }
        }
    }

    override fun observeAll(): Flow<List<StockMovementEntity>> {
        return flowOf(emptyList())
    }


    override fun search(query: String): Flow<List<StockMovementEntity>> {
        return flowOf(emptyList())
    }

    override fun findById(id: String): Flow<StockMovementEntity?> {
        return flowOf(null)
    }

    override suspend fun count(): Flow<Long?> {
        return manager.observeSingle { it.stockMovementQueries.count() }
    }

    override fun observeByBatch(batchId: String): Flow<List<StockMovementEntity>> {
        return manager.database.flatMapLatest { db ->
            db.stockMovementQueries.selectByBatch(batchId, "")
                .asFlow().mapToList(manager.dispatcher)
        }
    }

    override suspend fun getMaxCreatedAtForProduct(productId: String, branchId: String): Instant? {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.stockMovementQueries.getMaxCreatedAtForProduct(productId, branchId).executeAsOne().MAX
        }
    }

    override fun observeByProduct(
        productId: String,
        branchId: String?
    ): Flow<List<StockMovementEntity>> {
        val bid = branchId ?: ""
        val accessWindow = quotaGuard.calculateActiveSalesWindow()
        
        return manager.database.flatMapLatest { db ->
            val query = if (accessWindow.isUnrestricted || accessWindow.oldestAllowedTimestamp == null) {
                db.stockMovementQueries.selectByProduct(productId, bid)
            } else {
                db.stockMovementQueries.selectByProductFiltered(productId, bid, accessWindow.oldestAllowedTimestamp!!)
            }
            query.asFlow().mapToList(manager.dispatcher)
        }
    }
}
