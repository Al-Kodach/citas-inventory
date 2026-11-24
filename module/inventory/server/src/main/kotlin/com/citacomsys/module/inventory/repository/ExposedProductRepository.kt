package com.citacomsys.module.inventory.repository

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.query.queryPage
import com.citacomsys.core.persistence.table.inventory.BaseProductsTable
import com.citacomsys.core.persistence.table.inventory.ProductSnapshotViewTable
import com.citacomsys.core.persistence.table.inventory.StockBatchesTable
import com.citacomsys.core.persistence.table.inventory.ViewBatchSnapshotTable
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.module.inventory.model.ProductSnapshot
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.Money
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.ProductItem
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock
import kotlin.time.Instant

internal class ExposedProductRepository : ProductRepository {
    private val logger = KotlinLogging.logger { }

    override suspend fun saveBaseIdentity(product: BaseProduct) {
        BaseProductsTable.upsert {
            it[id] = product.id.toUuid()
            it[workspaceId] = product.workspaceId.toUuid()
            it[name] = product.name
            it[sku] = product.sku
            it[barcode] = product.barcode
            it[category] = product.category
            it[reorderLevel] = product.reorderLevel
            it[productType] = product.type
            it[isActive] = product.isActive
            it[brandName] = product.brandName
            it[description] = product.description
            it[unitOfMeasure] = product.unitOfMeasure
            it[createdAt] = product.createdAt
            it[updatedAt] = product.updatedAt
        }
    }

    override suspend fun fetch(workspaceId: String, query: PageQuery): PageResult<ProductSnapshot> {
        return ProductSnapshotViewTable.queryPage(
            pageQuery = query,
            searchColumns = listOf(
                ProductSnapshotViewTable.name,
                ProductSnapshotViewTable.sku,
                ProductSnapshotViewTable.barcode,
                ProductSnapshotViewTable.brandName
            ),
            filterBuilder = {
                this.andWhere { ProductSnapshotViewTable.workspaceId eq workspaceId.toUuid() }
                query.branchId?.let {
                    this.andWhere { ProductSnapshotViewTable.branchId eq it.toUuid() }
                }
                this
            },
            sortMap = mapOf(
                "ID" to ProductSnapshotViewTable.name,
                "stock" to ProductSnapshotViewTable.totalAvailable
            ),
            defaultSort = ProductSnapshotViewTable.name,
            mapper = { it.toProductSnapshot() }
        )
    }

    override suspend fun softDelete(
        workspaceId: String,
        productId: String
    ): Result<ProductSnapshot> =
        runCatching {
            val uuid = productId.toUuid()
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val product = getById(productId, workspaceId)

            val hasStocks = StockBatchesTable.select(StockBatchesTable.id)
                .where {
                    StockBatchesTable.productId eq productId.toUuid() and (
                            StockBatchesTable.workspaceId eq workspaceId.toUuid()
                            )
                }.count() > 0

            if (hasStocks.not()) {
                val deletedRows = BaseProductsTable.deleteWhere {
                    (BaseProductsTable.id eq uuid) and
                            (BaseProductsTable.workspaceId eq workspaceId.toUuid())
                }
                logger.warn("Deleted $deletedRows rows for product")
                if (deletedRows == 0) {
                    throw IllegalStateException("Product not found")
                }
                return@runCatching product!!
            }

            BaseProductsTable.update({ (BaseProductsTable.id eq uuid) and (BaseProductsTable.workspaceId eq workspaceId.toUuid()) }) {
                it[isActive] = false
                it[updatedAt] = Clock.System.now()
                it[sku] = CustomFunction(
                    "concat",
                    VarCharColumnType(),
                    BaseProductsTable.sku,
                    stringLiteral("_DEL_$timestamp")
                )
                it[barcode] = CustomFunction(
                    "concat",
                    VarCharColumnType(),
                    BaseProductsTable.barcode,
                    stringLiteral("_DEL_$timestamp")
                )
            }

            StockBatchesTable.update({ StockBatchesTable.productId eq uuid }) {
                it[isArchived] = true
                it[updatedAt] = Clock.System.now()

                it[batchNumber] = CustomFunction(
                    "concat",
                    VarCharColumnType(),
                    StockBatchesTable.batchNumber,
                    stringLiteral("_DEL_$timestamp")
                )
            }

            product!!
        }

    override suspend fun existsBySku(sku: String, workspaceId: String): Boolean = dbQuery {
        BaseProductsTable
            .select(BaseProductsTable.id)
            .where {
                (BaseProductsTable.sku eq sku) and
                        (BaseProductsTable.workspaceId eq workspaceId.toUuid()) and
                        (BaseProductsTable.isActive eq true)
            }
            .limit(1)
            .count() > 0
    }

    override suspend fun existsByBarcode(
        barcode: String,
        workspaceId: String
    ): Boolean = dbQuery {
        BaseProductsTable
            .select(BaseProductsTable.id)
            .where {
                (BaseProductsTable.barcode eq barcode) and
                        (BaseProductsTable.workspaceId eq workspaceId.toUuid()) and
                        (BaseProductsTable.isActive eq true)
            }
            .limit(1)
            .count() > 0
    }

    override suspend fun existById(productId: String, workspaceId: String): Boolean {
        return dbQuery {
            BaseProductsTable
                .select(BaseProductsTable.id)
                .where {
                    BaseProductsTable.id eq productId.toUuid() and
                            (BaseProductsTable.workspaceId eq workspaceId.toUuid())
                }
                .limit(1)
                .count() > 0
        }
    }

    override suspend fun getSnapshotChanges(
        since: Instant,
        organizationId: String,
        workspaceId: String,
        branchId: String
    ): ChangeListResponse<ProductSnapshot, String> = dbQuery {
        val orgUuid = organizationId.toUuid()
        val wsId = workspaceId.toUuid()

        val deletedRows = ProductSnapshotViewTable
            .innerJoin(Workspaces, { ProductSnapshotViewTable.workspaceId }, { Workspaces.id })
            .selectAll()
            .where {
                (Workspaces.organizationId eq orgUuid) and
                        (ProductSnapshotViewTable.workspaceId eq wsId) and
                        (ProductSnapshotViewTable.versionTimestamp greater since) and
                        (ProductSnapshotViewTable.branchId eq branchId.toUuid()) and
                        (ProductSnapshotViewTable.isActive eq false)
            }
            .map { it[ProductSnapshotViewTable.id].toString() }

        // Products are derived client-side from active StockBatchSnapshots via ProductListItemView.sq.
        // Product sync only transmits soft-deleted product IDs to purge deleted items from local storage.
        ChangeListResponse(
            updated = emptyList(),
            deleted = deletedRows
        )
    }

    override suspend fun getProductItemChanges(
        since: Instant,
        organizationId: String,
        workspaceId: String,
        branchId: String
    ): ChangeListResponse<ProductItem, String> = dbQuery {
        val orgUuid = organizationId.toUuid()
        val wsUuid = workspaceId.toUuid()
        val brUuid = branchId.toUuid()

        val deletedRows = BaseProductsTable
            .innerJoin(Workspaces, { BaseProductsTable.workspaceId }, { Workspaces.id })
            .selectAll()
            .where {
                (Workspaces.organizationId eq orgUuid) and
                        (BaseProductsTable.workspaceId eq wsUuid) and
                        (BaseProductsTable.updatedAt greater since) and
                        (BaseProductsTable.isActive eq false)
            }
            .map { it[BaseProductsTable.id].toString() }

        val updatedItems = ViewBatchSnapshotTable
            .innerJoin(BaseProductsTable, { ViewBatchSnapshotTable.productId }, { BaseProductsTable.id })
            .innerJoin(Workspaces, { ViewBatchSnapshotTable.workspaceId }, { Workspaces.id })
            .selectAll()
            .where {
                (Workspaces.organizationId eq orgUuid) and
                        (ViewBatchSnapshotTable.workspaceId eq wsUuid) and
                        (ViewBatchSnapshotTable.branchId eq brUuid) and
                        (ViewBatchSnapshotTable.versionTimestamp greater since) and
                        (ViewBatchSnapshotTable.productIsActive eq true) and
                        (ViewBatchSnapshotTable.batchIsArchived eq false)
            }
            .distinctBy { it[ViewBatchSnapshotTable.productId] }
            .map { row ->
                ProductItem(
                    id = row[ViewBatchSnapshotTable.productId].toString(),
                    workspaceId = row[ViewBatchSnapshotTable.workspaceId].toString(),
                    name = row[ViewBatchSnapshotTable.productName],
                    brandName = row[ViewBatchSnapshotTable.brandName],
                    sku = row[BaseProductsTable.sku],
                    barcode = row[BaseProductsTable.barcode],
                    unitOfMeasure = row[ViewBatchSnapshotTable.unitOfMeasure],
                    reorderLevel = row[BaseProductsTable.reorderLevel],
                    type = row[BaseProductsTable.productType],
                    isActive = row[ViewBatchSnapshotTable.productIsActive],
                    totalAvailable = row[ViewBatchSnapshotTable.availableQuantity],
                    currentPriceAmountMinor = row[ViewBatchSnapshotTable.sellingPriceMinor],
                    currentPriceCurrency = row[ViewBatchSnapshotTable.sellingPriceCurrency],
                    versionTimestamp = row[ViewBatchSnapshotTable.versionTimestamp]
                )
            }

        ChangeListResponse(
            updated = updatedItems,
            deleted = deletedRows
        )
    }

    override suspend fun getById(
        productId: String,
        workspaceId: String
    ): ProductSnapshot? = dbQuery {
        ProductSnapshotViewTable.selectAll()
            .where {
                ProductSnapshotViewTable.id eq productId.toUuid() and
                        (ProductSnapshotViewTable.workspaceId eq workspaceId.toUuid())
            }
            .singleOrNull()
            ?.toProductSnapshot()
    }

    override suspend fun countLowStock(workspaceId: String, branchId: String?): Long = dbQuery {
        val tid = workspaceId.toUuid()
        val bid = branchId?.toUuid()

        ProductSnapshotViewTable.selectAll()
            .where {
                val conditions = mutableListOf<Op<Boolean>>()
                conditions.add(ProductSnapshotViewTable.workspaceId eq tid)
                conditions.add(ProductSnapshotViewTable.isActive eq true)
                conditions.add(ProductSnapshotViewTable.totalAvailable lessEq ProductSnapshotViewTable.reorderLevel)

                if (bid != null) {
                    conditions.add(ProductSnapshotViewTable.branchId eq bid)
                }

                conditions.reduce { acc, op -> acc and op }
            }
            .count()
    }

    override suspend fun purgeProductAndAllAssociatedBatchesPermanently(
        productId: String,
        workspaceId: String
    ) {
        dbQuery {
            val uuid = productId.toUuid()
            val tid = workspaceId.toUuid()

            StockBatchesTable.deleteWhere {
                (StockBatchesTable.productId eq uuid) and
                        (StockBatchesTable.workspaceId eq tid)
            }
            BaseProductsTable.deleteWhere {
                (BaseProductsTable.id eq uuid) and (BaseProductsTable.workspaceId eq tid)
            }
        }
    }

    private fun ResultRow.toProductSnapshot() = ProductSnapshot(
        id = this[ProductSnapshotViewTable.id].toString(),
        name = this[ProductSnapshotViewTable.name],
        brandName = this[ProductSnapshotViewTable.brandName],
        sku = this[ProductSnapshotViewTable.sku],
        barcode = this[ProductSnapshotViewTable.barcode],
        unitOfMeasure = this[ProductSnapshotViewTable.unitOfMeasure],
        type = this[ProductSnapshotViewTable.originApp],
        totalAvailable = this[ProductSnapshotViewTable.totalAvailable],
        reorderLevel = this[ProductSnapshotViewTable.reorderLevel],
        workspaceId = this[ProductSnapshotViewTable.workspaceId].toString(),
        currentPrice = this[ProductSnapshotViewTable.currentPriceAmount]?.let {
            Money(it, this[ProductSnapshotViewTable.currentPriceCurrency]!!)
        },
        nextExpiry = this[ProductSnapshotViewTable.nextExpiry],
        versionTimestamp = this[ProductSnapshotViewTable.versionTimestamp]
    )
}
