package com.citacomsys.module.inventory.repository

import com.citacomsys.module.inventory.model.ProductSnapshot
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.ProductItem
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * The generic Base product repository
 */
interface ProductRepository {

    // Write side
    suspend fun saveBaseIdentity(product: BaseProduct)
    suspend fun softDelete(workspaceId: String, productId: String): Result<ProductSnapshot>

    // Identity checks (Internal use)
    suspend fun existsBySku(sku: String, workspaceId: String): Boolean
    suspend fun existsByBarcode(barcode: String, workspaceId: String): Boolean
    suspend fun existById(productId: String, workspaceId: String): Boolean


    suspend fun fetch(workspaceId: String, query: PageQuery): PageResult<ProductSnapshot>

    /**
     * Delta Sync: Returns snapshots of products for a specific workspace.
     * Anchored to organizationId to prevent IDOR attacks.
     */
    suspend fun getSnapshotChanges(
        since: Instant,
        organizationId: String,
        workspaceId: String,
        branchId: String
    ): ChangeListResponse<ProductSnapshot, String>

    suspend fun getProductItemChanges(
        since: Instant,
        organizationId: String,
        workspaceId: String,
        branchId: String
    ): ChangeListResponse<ProductItem, String>


    suspend fun getById(productId: String, workspaceId: String): ProductSnapshot?

    /**
     * Dashboard Metrics: Counts products with stock levels at or below reorder threshold.
     */
    suspend fun countLowStock(workspaceId: String, branchId: String?): Long

    /**
     * Permanent catastrophic deletion (Action Challenge Guarded)
     */
    suspend fun purgeProductAndAllAssociatedBatchesPermanently(productId: String, workspaceId: String)
}
