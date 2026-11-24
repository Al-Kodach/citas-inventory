package com.citacomsys.core.database.dao.sale

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.dao.BaseDao
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.database.PaymentEntity
import com.citacomsys.database.SaleEntity
import com.citacomsys.database.SaleItemEntity
import com.citacomsys.database.SaleTransactionView
import com.citacomsys.types.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface SalesDao : BaseDao<SaleEntity, SaleTransactionView, String> {
    suspend fun saveWithItems(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        payments: List<PaymentEntity>,
    )

    suspend fun delete(ids: List<String>)
    suspend fun getItemsBySale(saleId: String): List<SaleItemEntity>
    suspend fun getPaymentsBySale(saleId: String): List<PaymentEntity>
}

internal class SqlDelightSalesDao(
    private val manager: InventoryDatabaseManager,
    private val quotaGuard: WorkspaceResourceQuotaGuard,
    private val sessionManager: AppSessionManager
) : SalesDao {

    override fun findById(id: String): Flow<SaleEntity?> =
        manager.observeSingle { it.saleQueries.select(id) }

    override suspend fun count(): Flow<Long?> =
        manager.observeSingle { it.saleQueries.countSales() }

    override suspend fun saveWithItems(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        payments: List<PaymentEntity>
    ): Unit {
        val db = manager.getDatabase()
        db.transaction {
            db.saleQueries.insert(sale)
            payments.forEach { p ->
                db.saleQueries.insertOrUpdatePayment(p)
            }
            items.forEach { i ->
                db.saleQueries.insertOrUpdateSaleItem(i)
            }
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<SaleTransactionView>> =
        combine(
            manager.database,
            sessionManager.activeWorkspaceIdFlow,
            sessionManager.activeViewBranchId
        ) { db, currentWsId, currentBranchId ->
            Triple(db, currentWsId ?: "", currentBranchId)
        }.flatMapLatest { (db, activeWsId, activeBranchId) ->
            val accessWindow = quotaGuard.calculateActiveSalesWindow()
            val query = if (accessWindow.isUnrestricted) {
                db.saleQueries.selectAll(branchId = activeBranchId, wsId = activeWsId)
            } else {
                val cutoff = accessWindow.oldestAllowedTimestamp ?: Clock.System.now()
                    .minus(3, DateTimeUnit.MONTH, TimeZone.UTC)

                db.saleQueries.selectRecent(cutoff = cutoff, branchId = activeBranchId, wsId = activeWsId)
            }
            query.asFlow().mapToList(manager.dispatcher)
        }

    override suspend fun getItemsBySale(saleId: String): List<SaleItemEntity> {
        return manager.getDatabase().saleQueries.selectItemsBySaleId(saleId).executeAsList()
    }

    override suspend fun getPaymentsBySale(saleId: String): List<PaymentEntity> {
        return manager.getDatabase().saleQueries.selectPaymentsBySaleId(saleId).executeAsList()
    }

    @OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
    override fun search(query: String): Flow<List<SaleTransactionView>> =
        combine(
            manager.database,
            sessionManager.activeWorkspaceIdFlow,
            sessionManager.activeViewBranchId
        ) { db, currentWsId, currentBranchId ->
            Triple(db, currentWsId ?: "", currentBranchId)
        }.flatMapLatest { (db, activeWsId, activeBranchId) ->
            val accessWindow = quotaGuard.calculateActiveSalesWindow()
            val queryDb = if (accessWindow.isUnrestricted) {
                db.saleQueries.search(branchId = activeBranchId, query = query, wsId = activeWsId)
            } else {
                val cutoff = accessWindow.oldestAllowedTimestamp ?: Clock.System.now()
                    .minus(3, DateTimeUnit.MONTH, TimeZone.UTC)

                db.saleQueries.searchFiltered(
                    branchId = activeBranchId,
                    wsId = activeWsId,
                    query = query,
                    cutoff = cutoff
                )
            }
            queryDb.asFlow().mapToList(manager.dispatcher)
        }

    override suspend fun delete(ids: List<String>) {
        val db = manager.getDatabase()
        db.transaction {
            db.saleQueries.delete(ids)
        }
    }
}

val SaleTransactionView.totalAmount: Money
    get() = Money(total_amount_minor, total_amount_currency)
