package com.citacomsys.pharmacy.api.model

import com.citacomsys.types.BaseProduct
import com.citacomsys.types.ProductType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

typealias PharmacyProduct = Pharmacy

@Serializable
data class Pharmacy(
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
    override val type: ProductType = ProductType.PHARMACY,
    override val isActive: Boolean = true,
    override val createdAt: Instant,
    override val updatedAt: Instant,

    // Clinical Extensions (Pharmacy Domain Only)
    val genericName: String?,
    val dosageForm: String?,
    val strength: String?,
    val requiresPrescription: Boolean = false,
    val therapeuticClass: String? = null,
    val fdaNumber: String? = null
) : BaseProduct {

    val ext: PharmacyProductExt
        get() {
            return PharmacyProductExt(
                genericName = genericName,
                dosageForm = dosageForm,
                strength = strength,
                requiresPrescription = requiresPrescription,
                therapeuticClass = therapeuticClass,
                fdaNumber = fdaNumber
            )
        }
}

/**
 * The extension part for the client shared product entity
 */
@Serializable
data class PharmacyProductExt(
    val genericName: String? = null,
    val dosageForm: String? = null,
    val strength: String? = null,
    val requiresPrescription: Boolean = false,
    val therapeuticClass: String? = null,
    val fdaNumber: String? = null
)
