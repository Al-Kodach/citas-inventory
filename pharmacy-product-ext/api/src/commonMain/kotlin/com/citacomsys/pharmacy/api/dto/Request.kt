package com.citacomsys.pharmacy.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PharmacyMetadataUpdateRequest(
    val workspaceId: String,
    val name: String,
    val brandName: String,
    val sku: String?,
    val barcode: String?,
    val description: String?,
    val genericName: String?,
    val dosageForm: String?,
    val unitOfMeasure: String?,
    val category: String?,
    val reorderLevel: Int?,
    val strength: String?,
    val requiresPrescription: Boolean = false,
    val therapeuticClass: String? = null,
    val fdaNumber: String? = null
)