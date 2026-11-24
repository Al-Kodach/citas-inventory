package com.citacomsys.module.inventory.model

import com.citacomsys.types.Money
import com.citacomsys.types.ProductType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("ProductSnapshot")
data class ProductSnapshot(
    val id: String,
    val name: String,
    val workspaceId: String,
    val brandName: String,
    val sku: String?,
    val barcode: String?,
    val unitOfMeasure: String,
    val type: ProductType,
    val reorderLevel: Int,

    // Operational Data
    val totalAvailable: Int,
    val currentPrice: Money?,
    val nextExpiry: LocalDate?,

    // Sync Metadata
    val versionTimestamp: Instant
)