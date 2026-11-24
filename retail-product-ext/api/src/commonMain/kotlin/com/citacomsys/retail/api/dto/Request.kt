package com.citacomsys.retail.api.dto

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class RetailMetadataUpdateRequest(
    val workspaceId: String,
    val name: String,
    val brandName: String,
    val sku: String?,
    val barcode: String?,
    val description: String?,
    val unitOfMeasure: String?,
    val category: String?,
    val reorderLevel: Int?,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val updatedAt: Instant = Clock.System.now()
)
