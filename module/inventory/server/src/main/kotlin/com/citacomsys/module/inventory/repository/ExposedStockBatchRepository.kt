package com.citacomsys.module.inventory.repository

import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.inventory.BaseProductsTable
import com.citacomsys.core.persistence.table.inventory.ViewBatchSnapshotTable
import com.citacomsys.core.persistence.table.inventory.StockBatchesTable
import com.citacomsys.core.persistence.table.inventory.StockMovementsTable
import com.citacomsys.types.BatchId
import com.citacomsys.types.BatchItem
import com.citacomsys.types.BatchStockSnapshot
import com.citacomsys.types.StockBatch
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.Money
import com.citacomsys.types.generateUUID
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

internal class ExposedStockBatchRepository : StockBatchRepository {

    private fun ResultRow.toStockBatch(
        hasMovements: Boolean
    ): StockBatch {
        return StockBatch(
            id = this[StockBatchesTable.id].toString(),
            workspaceId = this[StockBatchesTable.workspaceId].toString(),
            branchId = this[StockBatchesTable.branchId].toString(),
            productId = this[StockBatchesTable.productId].toString(),
            batchNumber = this[StockBatchesTable.batchNumber],
            expiryDate = this[StockBatchesTable.expiryDate],
            initialQuantity = this[StockBatchesTable.initialQuantity],
            currentQuantity = this[StockBatchesTable.currentQuantity],
            costPrice = Money(
                this[StockBatchesTable.costPriceAmountMinor],
                this[StockBatchesTable.costPriceCurrency]
            ),
            sellingPrice = Money(
                this[StockBatchesTable.sellingPriceAmountMinor],
                this[StockBatchesTable.sellingPriceCurrency]
            ),
            hasMovements = hasMovements,
            createdAt = this[StockBatchesTable.createdAt],
            updatedAt = this[StockBatchesTable.updatedAt],
            isDeleted = this[StockBatchesTable.isArchived]
        )
    }

    override suspend fun save(batch: StockBatch): StockBatch {
        return StockBatchesTable.upsertReturning {
            it[this.id] = batch.id.toUuid()
            it[branchId] = batch.branchId.toUuid()
            it[this.productId] = batch.productId.toUuid()
            it[this.workspaceId] = batch.workspaceId.toUuid()
            it[this.batchNumber] = batch.batchNumber
            it[this.initialQuantity] = batch.initialQuantity

            it[this.costPriceAmountMinor] = batch.costPrice.amountMinor
            it[costPriceCurrency] = batch.costPrice.currencyCode
            it[this.sellingPriceAmountMinor] = batch.sellingPrice.amountMinor
            it[sellingPriceCurrency] = batch.sellingPrice.currencyCode

            it[this.expiryDate] = batch.expiryDate
            it[createdAt] = batch.createdAt
            it[updatedAt] = batch.createdAt
        }.single().toStockBatch(false)
    }

    override suspend fun isExist(
        batchNumber: String,
        productId: String,
        branchId: String,
        workspaceId: String
    ): Boolean {
        return StockBatchesTable.select(StockBatchesTable.id).where {
            StockBatchesTable.batchNumber eq batchNumber and
                    (StockBatchesTable.productId eq productId.toUuid()) and
                    (StockBatchesTable.workspaceId eq workspaceId.toUuid()) and
                    (StockBatchesTable.branchId eq branchId.toUuid())
        }.singleOrNull() != null
    }


    override suspend fun findById(batchId: String, workspaceId: String): StockBatch? {
        val hasMovements = StockMovementsTable.select(StockMovementsTable.id)
            .where { StockMovementsTable.batchId eq batchId.toUuid() }
            .limit(1).count().toInt() == 1

        return StockBatchesTable.selectAll()
            .where {
                StockBatchesTable.id eq batchId.toUuid() and
                        (StockBatchesTable.workspaceId eq workspaceId.toUuid())
            }
            .singleOrNull()
            ?.toStockBatch(hasMovements)
    }

    private fun ResultRow.toBatchItem() = BatchItem(
        batchId = this[ViewBatchSnapshotTable.batchId].toString(),
        productId = this[ViewBatchSnapshotTable.productId].toString(),
        workspaceId = this[ViewBatchSnapshotTable.workspaceId].toString(),
        branchId = this[ViewBatchSnapshotTable.branchId].toString(),
        batchNumber = this[ViewBatchSnapshotTable.batchNumber],
        availableQuantity = this[ViewBatchSnapshotTable.availableQuantity],
        sellingPriceAmountMinor = this[ViewBatchSnapshotTable.sellingPriceMinor],
        sellingPriceCurrency = this[ViewBatchSnapshotTable.sellingPriceCurrency],
        expiryDate = this[ViewBatchSnapshotTable.expiryDate]?.toString(),
        versionTimestamp = this[ViewBatchSnapshotTable.versionTimestamp]
    )

    override suspend fun getChangeList(
        since: Instant?,
        organizationId: String,
        workspaceId: String?,
        branchId: String?
    ): ChangeListResponse<BatchItem, String> {
        val tenantUuid = workspaceId?.toUuid()

        val query = ViewBatchSnapshotTable
            .selectAll()
            .where {
                val conditions = mutableListOf<Op<Boolean>>()
                tenantUuid?.let {
                    conditions.add(ViewBatchSnapshotTable.workspaceId eq it)
                }
                since?.let {
                    conditions.add(ViewBatchSnapshotTable.versionTimestamp greater it)
                }
                if (!branchId.isNullOrBlank()) {
                    conditions.add(ViewBatchSnapshotTable.branchId eq branchId.toUuid())
                }

                if (conditions.isEmpty()) Op.TRUE else conditions.reduce { acc, op -> acc and op }
            }

        val rawRows = query.toList()

        val (activeRows, deletedRows) = rawRows.partition { row ->
            row[ViewBatchSnapshotTable.productIsActive] && !row[ViewBatchSnapshotTable.batchIsArchived]
        }

        return ChangeListResponse(
            updated = activeRows.map { it.toBatchItem() },
            deleted = deletedRows.map { it[ViewBatchSnapshotTable.batchId].toString() }
        )
    }

    override suspend fun getExpiringBatches(
        workspaceId: String,
        thresholdDays: Int
    ): List<BatchStockSnapshot> = dbQuery {
        val tenantUuid = workspaceId.toUuid()
        val nowInstant = Clock.System.now()

        val timeZone = TimeZone.currentSystemDefault()
        val futureCutoffInstant = nowInstant.plus(thresholdDays.toDuration(DurationUnit.DAYS))
        val futureCutoffDateStr = futureCutoffInstant.toLocalDateTime(timeZone).date
        val todayDateStr = nowInstant.toLocalDateTime(timeZone).date

        ViewBatchSnapshotTable.selectAll()
            .where {
                (ViewBatchSnapshotTable.workspaceId eq tenantUuid) and
                        (ViewBatchSnapshotTable.productIsActive eq true) and
                        (ViewBatchSnapshotTable.batchIsArchived eq false) and
                        (ViewBatchSnapshotTable.availableQuantity greater 0) and
                        (ViewBatchSnapshotTable.expiryDate.isNotNull()) and
                        (ViewBatchSnapshotTable.expiryDate less futureCutoffDateStr) and
                        (ViewBatchSnapshotTable.expiryDate greater todayDateStr)
            }
            .orderBy(ViewBatchSnapshotTable.expiryDate to SortOrder.ASC)
            .map { row ->
                row.toSnapshot()
            }
    }

    override suspend fun countExpiryRisks(
        workspaceId: String,
        branchId: String?,
        thresholdDays: Int
    ): Long = dbQuery {
        val tenantUuid = workspaceId.toUuid()
        val bid = branchId?.toUuid()
        val nowInstant = Clock.System.now()

        val timeZone = TimeZone.currentSystemDefault()
        val futureCutoffInstant = nowInstant.plus(thresholdDays.toDuration(DurationUnit.DAYS))
        val futureCutoffDateStr = futureCutoffInstant.toLocalDateTime(timeZone).date
        val todayDateStr = nowInstant.toLocalDateTime(timeZone).date

        ViewBatchSnapshotTable.selectAll()
            .where {
                val conditions = mutableListOf<Op<Boolean>>()
                conditions.add(ViewBatchSnapshotTable.workspaceId eq tenantUuid)
                conditions.add(ViewBatchSnapshotTable.productIsActive eq true)
                conditions.add(ViewBatchSnapshotTable.batchIsArchived eq false)
                conditions.add(ViewBatchSnapshotTable.availableQuantity greater 0)
                conditions.add(ViewBatchSnapshotTable.expiryDate.isNotNull())
                conditions.add(ViewBatchSnapshotTable.expiryDate less futureCutoffDateStr)
                conditions.add(ViewBatchSnapshotTable.expiryDate greater todayDateStr)

                if (bid != null) {
                    conditions.add(ViewBatchSnapshotTable.branchId eq bid)
                }

                conditions.reduce { acc, op -> acc and op }
            }
            .count()
    }

    override suspend fun countActiveInventory(workspaceId: String, branchId: String?): Long =
        dbQuery {
            val tenantUuid = workspaceId.toUuid()
            val bid = branchId?.toUuid()

            val countColumn = ViewBatchSnapshotTable.productId.countDistinct()
            ViewBatchSnapshotTable.select(countColumn)
                .where {
                    val conditions = mutableListOf<Op<Boolean>>()
                    conditions.add(ViewBatchSnapshotTable.workspaceId eq tenantUuid)
                    conditions.add(ViewBatchSnapshotTable.productIsActive eq true)
                    conditions.add(ViewBatchSnapshotTable.availableQuantity greater 0)

                    if (bid != null) {
                        conditions.add(ViewBatchSnapshotTable.branchId eq bid)
                    }

                    conditions.reduce { acc, op -> acc and op }
                }
                .single()[countColumn]
        }

    override suspend fun findOrCreateMatchingBranchBatch(
        workspaceId: String,
        branchId: String,
        productId: String,
        batchNumber: String,
        expiryDate: LocalDate?,
        costPrice: Money,
        sellingPrice: Money
    ): String = dbQuery {
        val tenantUuid = workspaceId.toUuid()
        val branchUuid = branchId.toUuid()
        val productUuid = productId.toUuid()

        val existingBatchId = StockBatchesTable
            .select(StockBatchesTable.id)
            .where {
                (StockBatchesTable.workspaceId eq tenantUuid) and
                        (StockBatchesTable.branchId eq branchUuid) and
                        (StockBatchesTable.productId eq productUuid) and
                        (StockBatchesTable.batchNumber eq batchNumber)
            }
            .map { it[StockBatchesTable.id].toString() }
            .firstOrNull()

        if (existingBatchId != null) {
            return@dbQuery existingBatchId
        }

        val newBatchId = generateUUID()
        val now = Clock.System.now()

        StockBatchesTable.insert {
            it[id] = newBatchId.toUuid()
            it[StockBatchesTable.workspaceId] = tenantUuid
            it[StockBatchesTable.branchId] = branchUuid
            it[StockBatchesTable.productId] = productUuid
            it[StockBatchesTable.batchNumber] = batchNumber
            it[StockBatchesTable.expiryDate] = expiryDate

            it[costPriceAmountMinor] = costPrice.amountMinor
            it[costPriceCurrency] = costPrice.currencyCode
            it[sellingPriceAmountMinor] = sellingPrice.amountMinor
            it[sellingPriceCurrency] = sellingPrice.currencyCode

            it[initialQuantity] = 0
            it[createdAt] = now
            it[updatedAt] = now
        }

        newBatchId
    }


    override suspend fun updateBatchMetadata(
        batchId: String,
        newBatchNumber: String,
        newExpiry: LocalDate?,
        workspaceId: String
    ) {
        dbQuery {
            StockBatchesTable.update({
                StockBatchesTable.id eq batchId.toUuid() and
                        (StockBatchesTable.workspaceId eq workspaceId.toUuid())
            }) {
                it[batchNumber] = newBatchNumber
                it[expiryDate] = newExpiry
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun getBatchSnapshotsFIFO(
        workspaceId: String,
        productId: String,
        shouldLock: Boolean
    ): List<BatchStockSnapshot> = dbQuery {
        val wsUuid = workspaceId.toUuid()
        val productUuid = productId.toUuid()

        if (shouldLock) {
            BaseProductsTable.selectAll()
                .where { (BaseProductsTable.id eq productUuid) and (BaseProductsTable.workspaceId eq wsUuid) }
                .forUpdate()
                .toList()

            StockBatchesTable.selectAll()
                .where {
                    (StockBatchesTable.productId eq productUuid) and
                            (StockBatchesTable.workspaceId eq wsUuid) and
                            (StockBatchesTable.isArchived eq false)
                }
                .forUpdate()
                .toList()
        }

        ViewBatchSnapshotTable.selectAll()
            .where {
                (ViewBatchSnapshotTable.workspaceId eq wsUuid) and
                        (ViewBatchSnapshotTable.productId eq productUuid) and
                        (ViewBatchSnapshotTable.productIsActive eq true) and
                        (ViewBatchSnapshotTable.batchIsArchived eq false)
            }
            .orderBy(
                ViewBatchSnapshotTable.expiryDate to SortOrder.ASC,
                ViewBatchSnapshotTable.batchId to SortOrder.ASC
            )
            .map { it.toSnapshot() }
    }


    override suspend fun findSnapshotById(
        batchId: BatchId,
        workspaceId: String
    ): BatchStockSnapshot? = dbQuery {
        ViewBatchSnapshotTable.selectAll()
            .where {
                (ViewBatchSnapshotTable.workspaceId eq workspaceId.toUuid()) and
                        (ViewBatchSnapshotTable.batchId eq batchId.toUuid())
            }
            .singleOrNull()?.toSnapshot()
    }

    override suspend fun delete(
        workspaceId: String,
        batchId: String
    ): Boolean {
        val hasHistory = StockMovementsTable.select(StockMovementsTable.id)
            .where { StockMovementsTable.batchId eq batchId.toUuid() }
            .limit(1).count() > 0

        if (hasHistory) {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            return StockBatchesTable.update(
                where = {
                    StockBatchesTable.id eq batchId.toUuid() and
                            (StockBatchesTable.workspaceId eq workspaceId.toUuid())
                }
            ) {
                it[StockBatchesTable.isArchived] = true
                it[StockBatchesTable.updatedAt] = Clock.System.now()

                it[batchNumber] = CustomFunction(
                    "concat",
                    VarCharColumnType(),
                    StockBatchesTable.batchNumber,
                    stringLiteral("_DEL_$timestamp")
                )
            } == 1
        }

        StockBatchesTable.deleteWhere {
            StockBatchesTable.id eq batchId.toUuid() and
                    (StockBatchesTable.workspaceId eq workspaceId.toUuid())
        }

        return true
    }

    private fun ResultRow.toSnapshot(): BatchStockSnapshot {
        return BatchStockSnapshot(
            batchId = this[ViewBatchSnapshotTable.batchId].toString(),
            productId = this[ViewBatchSnapshotTable.productId].toString(),
            workspaceId = this[ViewBatchSnapshotTable.workspaceId].toString(),
            branchId = this[ViewBatchSnapshotTable.branchId].toString(),
            productName = this[ViewBatchSnapshotTable.productName],
            brandName = this[ViewBatchSnapshotTable.brandName],
            batchNumber = this[ViewBatchSnapshotTable.batchNumber],
            expiryDate = this[ViewBatchSnapshotTable.expiryDate],
            availableQuantity = this[ViewBatchSnapshotTable.availableQuantity],
            initialQuantity = this[ViewBatchSnapshotTable.initialQuantity],
            unitOfMeasure = this[ViewBatchSnapshotTable.unitOfMeasure],
            unitPrice = Money(
                this[ViewBatchSnapshotTable.sellingPriceMinor],
                this[ViewBatchSnapshotTable.sellingPriceCurrency],
            ),
            batchCreatedAt = this[ViewBatchSnapshotTable.batchCreatedAt],
            versionTimestamp = this[ViewBatchSnapshotTable.versionTimestamp],
        )
    }
}
