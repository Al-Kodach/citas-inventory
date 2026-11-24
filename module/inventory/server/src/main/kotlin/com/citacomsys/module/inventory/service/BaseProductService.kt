package com.citacomsys.module.inventory.service

import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuard
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.module.inventory.dto.ProductMetadataUpdateRequest
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.ProductItem
import kotlin.time.Clock
import kotlin.time.Instant

class BaseProductService(
    private val productRepository: ProductRepository,
    auditor: TelemetryAuditor,
    quotaGuard: WorkspaceQuotaGuard
) : ProductService<ProductItem>(productRepository, auditor, quotaGuard) {

    override suspend fun saveExtensionData(product: ProductItem) {
        // Base products have no extension table data
    }

    override suspend fun getById(productId: String, workspaceId: String): ProductItem? = dbQuery {
        val snapshot = productRepository.getById(productId, workspaceId) ?: return@dbQuery null
        ProductItem(
            id = snapshot.id,
            workspaceId = snapshot.workspaceId,
            name = snapshot.name,
            brandName = snapshot.brandName,
            sku = snapshot.sku,
            barcode = snapshot.barcode,
            unitOfMeasure = snapshot.unitOfMeasure,
            reorderLevel = snapshot.reorderLevel,
            type = snapshot.type,
            versionTimestamp = snapshot.versionTimestamp
        )
    }

    suspend fun getProductItemChanges(
        since: Instant,
        organizationId: String,
        workspaceId: String,
        branchId: String
    ): ChangeListResponse<ProductItem, String> {
        return productRepository.getProductItemChanges(since, organizationId, workspaceId, branchId)
    }

    suspend fun updateMetadata(
        id: String,
        workspaceId: String,
        request: ProductMetadataUpdateRequest
    ): ProductItem = dbQuery {
        val existing = getById(id, workspaceId) ?: error("Product $id not found in workspace $workspaceId")
        val updated = existing.copy(
            name = request.name,
            brandName = request.brandName ?: existing.brandName,
            sku = request.sku,
            barcode = request.barcode,
            description = request.description,
            unitOfMeasure = request.unitOfMeasure ?: existing.unitOfMeasure,
            category = request.category,
            reorderLevel = request.reorderLevel ?: existing.reorderLevel,
            versionTimestamp = Clock.System.now()
        )

        saveProduct(updated, workspaceId)
        updated
    }
}
