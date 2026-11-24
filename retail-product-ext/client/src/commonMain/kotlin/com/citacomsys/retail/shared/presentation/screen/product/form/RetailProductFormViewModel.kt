package com.citacomsys.retail.shared.presentation.screen.product.form

import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.module.inventory.ui.screen.product.BaseProductFormViewModel
import com.citacomsys.module.inventory.ui.screen.product.ProductTemplate
import com.citacomsys.retail.api.model.Retail
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.retail.shared.data.repository.ClientRetailRepository
import com.citacomsys.types.ProductType
import com.citacomsys.types.ResourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

class RetailProductFormViewModel(
    notification: InAppNotificationService,
    private val productRepo: ClientRetailRepository,
    sessionManager: AppSessionManager,
    telemetryClient: TelemetryLogClient,
    private val quotaGuard: WorkspaceResourceQuotaGuard,
) : BaseProductFormViewModel<RetailProduct>(notification, sessionManager, telemetryClient) {

    // --- Retail Specific State ---
    val tags = MutableStateFlow<List<String>>(emptyList())
    val attributes = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        observeFormFieldsForAutoSku()
    }

    override suspend fun getById(id: String): Result<RetailProduct> {
        return productRepo.getById(id)
    }

    override suspend fun create(product: RetailProduct): Result<Unit> = runCatching {
        quotaGuard.verifyProductCreationAllowed()
        val user = sessionManager.appUser.value ?: error("No active user session")

        productRepo.createProduct(
            product.copy(
                id = newProductId,
                workspaceId = sessionManager.activeWorkspaceId!!,
                type = ProductType.RETAIL
            ),
            actorId = user.id,
            actorName = user.name,
        ).getOrThrow()

        quotaGuard.consumeLocalResource(ResourceType.PRODUCT)
    }

    override suspend fun update(product: RetailProduct): Result<Unit> {
        return Result.success(Unit)
    }

    override fun mapProductToForm(product: RetailProduct) {
        super.mapProductToForm(product)
        tags.value = product.tags
        attributes.value = product.attributes
    }

    override fun createProductFromForm(
        currentProduct: RetailProduct?,
        now: Instant
    ): RetailProduct {
        return Retail(
            id = currentProduct?.id ?: "",
            workspaceId = currentProduct?.workspaceId ?: "",
            name = name.value.trim(),
            brandName = brandName.value.trim(),
            sku = resolveSku(),
            barcode = barcode.value.trim().takeIf { it.isNotBlank() },
            description = description.value.trim().takeIf { it.isNotBlank() },
            unitOfMeasure = unitOfMeasure.value.trim().ifEmpty { "Piece" },
            category = category.value.trim().takeIf { it.isNotBlank() },
            reorderLevel = reorderLevel.value.trim().toIntOrNull() ?: 10,
            isActive = currentProduct?.isActive ?: true,
            createdAt = currentProduct?.createdAt ?: now,
            type = ProductType.RETAIL,
            updatedAt = now,
            tags = tags.value,
            attributes = attributes.value
        )
    }

    override fun clearForm() {
        super.clearForm()
        tags.value = emptyList()
        attributes.value = emptyMap()
    }

    override fun loadTemplates() {
        _templates.value = listOf(
            ProductTemplate(
                name = "V-Neck T-Shirt Blue Medium",
                category = "Apparel"
            ),
            ProductTemplate(
                name = "Cotton T-Shirt Black Large",
                category = "Apparel"
            ),
            ProductTemplate(
                name = "Wireless Bluetooth Headphones",
                category = "Electronics"
            ),
            ProductTemplate(
                name = "USB-C Fast Charger 20W",
                category = "Electronics"
            ),
            ProductTemplate(
                name = "Organic Whole Milk 1L",
                category = "Grocery"
            ),
            ProductTemplate(
                name = "Generic Retail Item",
                category = "General"
            )
        )
    }
}
