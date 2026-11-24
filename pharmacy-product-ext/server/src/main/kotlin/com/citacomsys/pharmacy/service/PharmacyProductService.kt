package com.citacomsys.pharmacy.service

import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuard
import com.citacomsys.module.inventory.service.ProductService
import com.citacomsys.pharmacy.api.PharmacyProductRepository
import com.citacomsys.pharmacy.api.dto.PharmacyMetadataUpdateRequest
import com.citacomsys.pharmacy.api.model.PharmacyProduct

internal class PharmacyProductService(
    private val pharmacyRepo: PharmacyProductRepository,
    auditor: TelemetryAuditor,
    quotaGuard: WorkspaceQuotaGuard
) : ProductService<PharmacyProduct>(
    pharmacyRepo, auditor, quotaGuard
) {

    override suspend fun saveExtensionData(product: PharmacyProduct) {
        pharmacyRepo.savePharmacyProduct(product)
    }

    override suspend fun getById(
        productId: String,
        workspaceId: String
    ): PharmacyProduct? {
        return pharmacyRepo.getPharmacyProduct(productId, workspaceId)
    }

    suspend fun updateMetadata(
        id: String,
        workspaceId: String,
        request: PharmacyMetadataUpdateRequest,
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
            genericName = request.genericName,
            dosageForm = request.dosageForm,
            strength = request.strength,
            requiresPrescription = request.requiresPrescription,
            therapeuticClass = request.therapeuticClass,
            fdaNumber = request.fdaNumber
        )

        pharmacyRepo.updatePharmacyProduct(
            product = updatedProduct,
            workspaceId = workspaceId
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
