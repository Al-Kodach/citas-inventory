package com.citacomsys.retail.api.model

import com.citacomsys.types.BaseProduct
import com.citacomsys.types.ProductType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

typealias RetailProduct = Retail

@Serializable
data class Retail(
    override val id: String,
    override val workspaceId: String,
    override val name: String,
    override val brandName: String,
    override val sku: String?,
    override val barcode: String?,
    override val unitOfMeasure: String = "Piece",
    override val category: String?,
    override val description: String? = null,
    override val reorderLevel: Int = 10,
    override val type: ProductType = ProductType.RETAIL,
    override val isActive: Boolean = true,
    override val createdAt: Instant,
    override val updatedAt: Instant,

    // General Purpose Extension (Minimalist)
    val tags: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap()
) : BaseProduct {

    val ext: RetailProductExt
        get() = RetailProductExt(
            tags = tags,
            attributes = attributes
        )
}

@Serializable
data class RetailProductExt(
    val tags: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap()
)
