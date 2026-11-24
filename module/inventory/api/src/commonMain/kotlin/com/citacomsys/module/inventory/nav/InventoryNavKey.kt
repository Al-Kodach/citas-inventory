package com.citacomsys.module.inventory.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable


/**
 * Localized feature marker. Instead of inheriting from InventoryNav directly
 * (which is an object), it inherits from the top-level boundary class (TenantNavKey).
 */
@Serializable
sealed interface InventoryModuleNavKey : NavKey {
    val productId: String?
        get() = null
}


@Serializable
data class ProductFormNav(override val productId: String? = null) : InventoryModuleNavKey

@Serializable
data class ProductMetadataNav(override val productId: String) : InventoryModuleNavKey

@Serializable
data class ProductStocksNavKey(override val productId: String) : InventoryModuleNavKey

@Serializable
data class CreateBatchNavKey(override val productId: String) : InventoryModuleNavKey

@Serializable
data class EditBatchNav(override val productId: String, val batchId: String) : InventoryModuleNavKey

@Serializable
data class StockTransferNavKey(
    override val productId: String,
    val batchId: String
) : InventoryModuleNavKey

@Serializable
data class BatchDetailsNavKey(
    override val productId: String,
    val batchId: String
) : InventoryModuleNavKey




