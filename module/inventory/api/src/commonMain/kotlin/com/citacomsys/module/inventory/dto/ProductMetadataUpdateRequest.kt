package com.citacomsys.module.inventory.dto

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class ProductMetadataUpdateRequest(
    val workspaceId: String,
    val name: String,
    val brandName: String? = null,
    val sku: String? = null,
    val barcode: String? = null,
    val description: String? = null,
    val unitOfMeasure: String? = null,
    val category: String? = null,
    val reorderLevel: Int? = null,
    val updatedAt: Instant = Clock.System.now()
)
