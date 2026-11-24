package com.citacomsys.types

import kotlinx.serialization.Serializable
import kotlin.time.Instant

interface BaseProduct {
    val id: String
    val workspaceId: String
    val sku: String?
    val barcode: String?
    val name: String
    val brandName: String
    val description: String?
    val category: String?
    val unitOfMeasure: String   // "Box", "Piece", "Bottle" - Stock math needs context
    val reorderLevel: Int
    val type: ProductType
    val isActive: Boolean
    val createdAt: Instant
    val updatedAt: Instant
}

@Serializable
data class ProductInventoryState(
    val productId: String,
    val totalAvailable: Int,
    val versionMarker: Instant
)

/**
 * Ultra-minimal Product Master item for fast POS & Management catalog sync.
 * Omits heavy description/category fields during network sync (loaded on-demand in details screen).
 */
@Serializable
data class ProductItem(
    override val id: String,
    override val workspaceId: String,
    override val name: String,
    override val brandName: String,
    override val sku: String? = null,
    override val barcode: String? = null,
    override val category: String? = null,
    override val description: String? = null,
    override val unitOfMeasure: String = "Piece",
    override val reorderLevel: Int = 0,
    override val type: ProductType,
    override val isActive: Boolean = true,
    val totalAvailable: Int = 0,               // Stock badge indicator
    val currentPriceAmountMinor: Long = 0L,    // Base selling price
    val currentPriceCurrency: String = "GHS",
    val versionTimestamp: Instant
) : BaseProduct {
    override val createdAt: Instant get() = versionTimestamp
    override val updatedAt: Instant get() = versionTimestamp
}
