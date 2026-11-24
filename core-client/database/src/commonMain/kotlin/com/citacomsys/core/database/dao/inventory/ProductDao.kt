package com.citacomsys.core.database.dao.inventory

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.database.ProductEntity
import com.citacomsys.database.ProductListItemView
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.types.PageQuery
import kotlinx.coroutines.flow.Flow
import kotlin.time.ExperimentalTime

interface ProductDao {
    /**
     * Observes the raw management metadata for a product.
     */
    fun findById(id: String): Flow<ProductEntity?>

    /**
     * Observes the operational snapshot for a product (Quantity, Price, Expiry).
     */
    fun find(id: String): Flow<ProductListItemView?>

    /**
     * Counts active products in the current workspace.
     */
    suspend fun count(): Long

    /**
     * Observes all visible products for the current workspace and active branch.
     */
    fun observeAll(): Flow<List<ProductListItemView>>
    fun observeAll(query: PageQuery): Flow<List<ProductListItemView>>

    /**
     * Observes products specifically for the current branch's POS context.
     */
    fun observePosProducts(): Flow<List<ProductListItemView>>

    /**
     * Searches for products across the organization's workspace.
     */
    fun search(query: String, branchId: String? = null): Flow<List<ProductListItemView>>

    /**
     * Saves or updates product management metadata.
     */
    suspend fun save(entity: ProductEntity)
}

@OptIn(ExperimentalTime::class)
internal class SqlDelightProductDao(
    private val manager: InventoryDatabaseManager,
    private val sessionManager: AppSessionManager
) : ProductDao {

    private val workspaceId: String
        get() = sessionManager.activeWorkspaceId ?: ""

    override fun findById(id: String): Flow<ProductEntity?> =
        manager.observeSingle { it.productQueries.selectById(id) }

    override fun find(id: String): Flow<ProductListItemView?> = manager.observeSingle {
        it.productListItemViewQueries.findById(
            productId_ = id,
            mapper = ::mapToProductListItemView
        )
    }

    override suspend fun count(): Long =
        manager.getDatabase().productItemQueries.count(workspaceId).executeAsOne()

    override fun observeAll(): Flow<List<ProductListItemView>> {
        val branchId = sessionManager.activeViewBranchId.value

        return manager.observesList {
            it.productListItemViewQueries.selectAll(
                workspaceId_ = workspaceId,
                branchId_ = branchId,
                mapper = ::mapToProductListItemView
            )
        }
    }

    override fun observeAll(query: PageQuery): Flow<List<ProductListItemView>> {
        val branchId = sessionManager.activeViewBranchId.value
        val searchQuery = query.search ?: ""
        return if (searchQuery.isBlank()) {
            manager.observesList { db ->
                db.productListItemViewQueries.selectAll(
                    workspaceId_ = workspaceId,
                    branchId_ = branchId,
                    mapper = ::mapToProductListItemView
                )
            }
        } else {
            manager.observesList { db ->
                db.productListItemViewQueries.search(
                    workspaceId_ = workspaceId,
                    branchId_ = branchId,
                    query = searchQuery,
                    mapper = ::mapToProductListItemView
                )
            }
        }
    }

    override fun observePosProducts(): Flow<List<ProductListItemView>> {
        val branchId = sessionManager.resolveWriteBranchId()
        return manager.observesList {
            it.productListItemViewQueries.selectAll(
                workspaceId_ = workspaceId,
                branchId_ = branchId,
                mapper = ::mapToProductListItemView
            )
        }
    }

    override fun search(
        query: String,
        branchId: String?
    ): Flow<List<ProductListItemView>> {
        return manager.observesList {
            it.productListItemViewQueries.search(
                workspaceId_ = workspaceId,
                query = query,
                mapper = ::mapToProductListItemView,
                branchId_ = branchId
            )
        }
    }

    override suspend fun save(entity: ProductEntity) {
        val db = manager.getDatabase()
        db.transaction {
            db.productQueries.insertOrReplace(entity)
        }
    }

    private fun mapToProductListItemView(
        id: String,
        workspaceId: String,
        branchId: String,
        name: String,
        brandName: String,
        unitOfMeasure: String,
        totalAvailable: Long,
        currentPriceAmount: Long,
        currentPriceCurrency: String,
        nextExpiry: String?,
        sku: String?,
        barcode: String?,
        productType: String,
        reorderLevel: Int,
    ): ProductListItemView {
        return ProductListItemView(
            id = id,
            workspaceId = workspaceId,
            branchId = branchId,
            name = name,
            brandName = brandName,
            unitOfMeasure = unitOfMeasure,
            totalAvailable = totalAvailable,
            currentPriceAmount = currentPriceAmount,
            currentPriceCurrency = currentPriceCurrency,
            nextExpiry = nextExpiry,
            sku = sku,
            barcode = barcode,
            productType = productType,
            reorderLevel = reorderLevel
        )
    }
}
