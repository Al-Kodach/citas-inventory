package com.citacomsys.retail.service

import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuard
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.retail.api.RetailProductRepository
import com.citacomsys.module.inventory.service.ProductService
import com.citacomsys.retail.api.dto.RetailMetadataUpdateRequest

internal class RetailProductService(
    private val repository: RetailProductRepository,
    auditor: TelemetryAuditor,
    quotaGuard: WorkspaceQuotaGuard
) : ProductService<RetailProduct>(repository, auditor, quotaGuard) {

    override suspend fun saveExtensionData(product: RetailProduct) {
        repository.saveRetailProduct(product)
    }

    override suspend fun getById(
        productId: String,
        workspaceId: String
    ): RetailProduct? {
        return repository.getRetailProduct(productId, workspaceId)
    }

    suspend fun updateMetadata(
        id: String,
        workspaceId: String,
        request: RetailMetadataUpdateRequest,
    ) {
        if (request.workspaceId != workspaceId) {
            throw IllegalArgumentException("Workspace mismatch in metadata update")
        }
        val product =
            getById(id, workspaceId) ?: throw IllegalArgumentException("Product not found")

        val updatedProduct = product.copy(
            name = request.name,
            brandName = request.brandName,
            sku = request.sku,
            barcode = request.barcode,
            description = request.description,
            unitOfMeasure = request.unitOfMeasure ?: product.unitOfMeasure,
            attributes = request.attributes,
            tags = request.tags,
            reorderLevel = request.reorderLevel ?: product.reorderLevel,
            category = request.category ?: product.category
        )

        repository.saveRetailProduct(
            product = updatedProduct
        )

        auditor.logApplicationEvent(
            action = "PRODUCT_METADATA_UPDATED",
            entityType = "PRODUCT",
            entityId = updatedProduct.id,
            summary = "Product metadata updated",
            level = TelemetryLevel.INFO,
            workspaceIdOverride = workspaceId
        )
    }
}
