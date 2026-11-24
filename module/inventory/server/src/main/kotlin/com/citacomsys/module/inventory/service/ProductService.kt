package com.citacomsys.module.inventory.service

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuard
import com.citacomsys.shared.error.DuplicateRecordException
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.module.inventory.model.ProductSnapshot
import com.citacomsys.shared.error.AuthorizationException
import com.citacomsys.types.*
import kotlin.time.Instant

abstract class ProductService<T : BaseProduct>(
    private val productRepository: ProductRepository,
    val auditor: TelemetryAuditor,
    private val quotaGuard: WorkspaceQuotaGuard
) {
    /**
     * HOOK: Subclasses MUST implement this to save their specific
     * metadata (e.g., Pharmacy clinical fields) into their extension tables.
     */
    protected abstract suspend fun saveExtensionData(product: T)

    /**
     * HOOK: Subclasses implement this to read their specific table extensions
     * without breaking the generic base pipeline.
     */
    abstract suspend fun getById(productId: String, workspaceId: String): T?

    /**
     * Core logic to register a product: Quota -> Base Save -> Extension Hook.
     */
    private suspend fun internalSave(product: T, isNew: Boolean): Unit = dbQuery {
        if (isNew) {
            quotaGuard.ensureAvailability(product.workspaceId, ResourceType.PRODUCT)
        }

        // Check for duplicates (Generic Identity)
        checkDuplicate(product)

        // Save generic base identity (ID, SKU, Barcode, etc)
        productRepository.saveBaseIdentity(product)

        // Trigger the app-specific extension save
        saveExtensionData(product)
    }

    suspend fun saveProduct(product: T, workspaceId: String): T {
        val workspaceId = workspaceId
        if (product.workspaceId != workspaceId) {
            throw AuthorizationException("Security scope validation broken")
        }

        internalSave(product, isNew = true)

        auditor.logApplicationEvent(
            action = "PRODUCT_CREATED",
            entityType = EntityType.PRODUCT.name,
            entityId = product.id,
            summary = "Product '${product.name}' initialized in system catalog via ${product.type}",
            level = TelemetryLevel.INFO
        )

        return product
    }

    suspend fun deleteProduct(
        productId: String,
        workspaceId: String,
        actorId: String
    ): Result<ProductSnapshot> = dbQuery {
        val result = productRepository.softDelete(workspaceId, productId)

        result.onSuccess { deletedProduct ->
            auditor.logApplicationEvent(
                action = "PRODUCT_DELETED",
                entityType = EntityType.PRODUCT.name,
                entityId = productId,
                summary = "Deleted product '${deletedProduct.name}' from inventory catalog",
                level = TelemetryLevel.WARNING,
                workspaceIdOverride = workspaceId
            )
        }.onFailure { error ->
            auditor.logApplicationEvent(
                action = "PRODUCT_DELETE_FAILED",
                entityType = EntityType.PRODUCT.name,
                entityId = productId,
                summary = "Failed to delete product '$productId': ${error.message}",
                level = TelemetryLevel.ERROR,
                workspaceIdOverride = workspaceId
            )
        }

        result
    }

    suspend fun purgeProductAndAllAssociatedBatchesPermanently(
        productId: String,
        workspaceId: String,
        actorId: String
    ) {
        productRepository.purgeProductAndAllAssociatedBatchesPermanently(productId, workspaceId)

        auditor.logApplicationEvent(
            action = "PRODUCT_PURGED",
            entityType = EntityType.PRODUCT.name,
            entityId = productId,
            summary = "Permanently purged product '$productId' and all associated batches",
            level = TelemetryLevel.WARNING,
            workspaceIdOverride = workspaceId
        )
    }

    // --- Generic Reads (Used by all apps) ---

    suspend fun getSnapshotChanges(
        since: Instant,
        organizationId: String,
        workspaceId: String,
        branchId: String
    ): ChangeListResponse<ProductSnapshot, String> {
        return productRepository.getSnapshotChanges(since, organizationId, workspaceId, branchId)
    }

    private suspend fun checkDuplicate(product: BaseProduct) {
        if (product.barcode != null && productRepository.existsByBarcode(
                product.barcode!!,
                product.workspaceId
            )
        ) {
            throw DuplicateRecordException(
                "barcode",
                "Barcode '${product.barcode}' is already in use."
            )
        }
        if (product.sku != null && productRepository.existsBySku(
                product.sku!!,
                product.workspaceId
            )
        ) {
            throw DuplicateRecordException("sku", "SKU '${product.sku}' is already in use.")
        }
    }

    suspend fun fetch(workspaceId: String, query: PageQuery) =
        productRepository.fetch(workspaceId, query)
}
