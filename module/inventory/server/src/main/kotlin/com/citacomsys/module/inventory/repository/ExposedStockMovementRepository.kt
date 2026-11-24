package com.citacomsys.module.inventory.repository

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.inventory.BaseProductsTable
import com.citacomsys.core.persistence.table.inventory.StockBatchesTable
import com.citacomsys.core.persistence.table.inventory.StockMovementsTable
import com.citacomsys.core.persistence.table.inventory.ViewBatchSnapshotTable
import com.citacomsys.core.persistence.table.tenant.WorkspaceAccountsTable
import com.citacomsys.core.persistence.table.tenant.WorkspaceResources
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuard
import com.citacomsys.types.ProductInventoryState
import com.citacomsys.types.StockMovement
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant

internal class ExposedStockMovementRepository(
    private val quotaGuard: WorkspaceQuotaGuard
) : StockMovementRepository {
    private val logger = mu.KotlinLogging.logger { }

    private fun ResultRow.toStockMovement() = StockMovement(
        id = this[StockMovementsTable.id],
        workspaceId = this[StockMovementsTable.workspaceId].toString(),
        productId = this[StockMovementsTable.productId].toString(),
        batchId = this[StockMovementsTable.batchId].toString(),
        branchId = this[StockMovementsTable.branchId].toString(),
        type = this[StockMovementsTable.type],
        quantity = this[StockMovementsTable.quantity],
        referenceId = this[StockMovementsTable.referenceId],
        referenceType = this[StockMovementsTable.referenceType],
        batchNumber = this[StockMovementsTable.batchNumber],
        actorType = this[StockMovementsTable.actorType],
        createdAt = Instant.fromEpochMilliseconds(this[StockMovementsTable.createdAt])
    )

    override suspend fun create(movement: StockMovement) {
        StockMovementsTable.insert {
            it[id] = movement.id
            it[workspaceId] = movement.workspaceId.toUuid()
            it[productId] = movement.productId.toUuid()
            it[branchId] = movement.branchId.toUuid()
            it[batchId] = movement.batchId.toUuid()
            it[type] = movement.type
            it[quantity] = movement.quantity
            it[referenceId] = movement.referenceId
            it[referenceType] = movement.referenceType
            it[batchNumber] = movement.batchNumber
            it[actorType] = movement.actorType
            it[createdAt] = movement.createdAt.toEpochMilliseconds()
        }
    }

    override suspend fun saveAll(movements: List<StockMovement>) {
        StockMovementsTable.batchInsert(movements) { movement ->
            this[StockMovementsTable.id] = movement.id
            this[StockMovementsTable.workspaceId] = movement.workspaceId.toUuid()
            this[StockMovementsTable.productId] = movement.productId.toUuid()
            this[StockMovementsTable.branchId] = movement.branchId.toUuid()
            this[StockMovementsTable.batchId] = movement.batchId.toUuid()
            this[StockMovementsTable.type] = movement.type
            this[StockMovementsTable.quantity] = movement.quantity
            this[StockMovementsTable.referenceId] = movement.referenceId
            this[StockMovementsTable.referenceType] = movement.referenceType
            this[StockMovementsTable.batchNumber] = movement.batchNumber
            this[StockMovementsTable.actorType] = movement.actorType
            this[StockMovementsTable.createdAt] = movement.createdAt.toEpochMilliseconds()
        }
    }

    override suspend fun getStockMovementsForProduct(
        productId: String,
        batchId: String,
        since: Instant?,
        workspaceId: String,
        branchId: String?
    ): List<StockMovement> = dbQuery {
        val tire = WorkspaceResources.select(WorkspaceResources.tier)
            .where { WorkspaceResources.workspaceId eq workspaceId.toUuid() }
            .single()[WorkspaceResources.tier]

        val window = quotaGuard.calculateLedgerWindow(tire)
        val createdAt =
            since?.toEpochMilliseconds() ?: Instant.fromEpochMilliseconds(0).toEpochMilliseconds()

        val query = StockMovementsTable.selectAll()
            .where {
                (StockMovementsTable.createdAt greater createdAt) and
                        (StockMovementsTable.workspaceId eq workspaceId.toUuid()) and
                        (StockMovementsTable.productId eq productId.toUuid()) and
                        (StockMovementsTable.batchId eq batchId.toUuid())
            }

        if (branchId != null) {
            query.andWhere { StockMovementsTable.branchId eq branchId.toUuid() }
        }

        query
            .andWhere {
                if (window.isUnrestricted || window.oldestAllowedTimestamp == null) {
                    Op.TRUE
                } else {
                    StockMovementsTable.createdAt greaterEq window.oldestAllowedTimestamp!!
                }
            }
            .map { it.toStockMovement() }
    }

    override suspend fun findDirtyProductIds(since: Instant, workspaceId: String): Set<String> =
        dbQuery {
            val tId = workspaceId.toUuid()

            val idsFromProducts = BaseProductsTable
                .select(BaseProductsTable.id)
                .where { (BaseProductsTable.workspaceId eq tId) and (BaseProductsTable.updatedAt greater since) }
                .map { it[BaseProductsTable.id].toString() }


            val idsFromBatches = StockBatchesTable
                .select(StockBatchesTable.productId)
                .where { (StockBatchesTable.workspaceId eq tId) and (StockBatchesTable.updatedAt greater since) }
                .map { it[StockBatchesTable.productId].toString() }


            val idsFromMovements = StockMovementsTable
                .select(StockMovementsTable.productId)
                .where {
                    (StockMovementsTable.workspaceId eq tId) and
                            (StockMovementsTable.createdAt greater since.toEpochMilliseconds())
                }
                .map { it[StockMovementsTable.productId].toString() }

            (idsFromProducts + idsFromBatches + idsFromMovements).toSet()
        }

    override suspend fun getProductsState(
        workspaceId: String,
        productIds: List<String>
    ): List<ProductInventoryState> {
        val uuids = productIds.map { it.toUuid() }

        return ViewBatchSnapshotTable
            .select(
                ViewBatchSnapshotTable.productId,
                ViewBatchSnapshotTable.availableQuantity.sum(),
                ViewBatchSnapshotTable.versionTimestamp.max()
            )
            .where {
                (ViewBatchSnapshotTable.workspaceId eq workspaceId.toUuid()) and
                        (ViewBatchSnapshotTable.productId inList uuids)
            }
            .groupBy(ViewBatchSnapshotTable.productId)
            .map { row ->
                ProductInventoryState(
                    productId = row[ViewBatchSnapshotTable.productId].toString(),
                    totalAvailable = row[ViewBatchSnapshotTable.availableQuantity.sum()] ?: 0,
                    versionMarker = row[ViewBatchSnapshotTable.versionTimestamp]
                )
            }
    }

}
