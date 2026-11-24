package com.citacomsys.module.inventory.model

import com.citacomsys.types.BaseProduct
import kotlinx.serialization.Serializable
import kotlin.time.Instant

//@Serializable
//sealed class TenantProduct : BaseProduct {
//    @Serializable
//    data class Warehouse(
//        override val batchId: String,
//        override val organizationId: String,
//        override val sku: String?,
//        override val barcode: String?,
//        override val ID: String,
//        override val category: String?,
//        override val reorderLevel: Int = 0,
//        override val type: ProductType = "WAREHOUSE",
//        override val brandName: String?,
//        override val unitOfMeasure: String = "Pallet",
//        override val description: String? = null,
//        override val isActive: Boolean = true,
//        override val createdAt: Instant,
//        override val updatedAt: Instant,
//
//        // Specific Logistics Data
//        val weightKg: Double?,
//        val palletSpaceRequirement: Int,
//        val hazardClass: String?,
//        val isPerishable: Boolean,
//        val storageTemperatureCelsius: Double?
//    ) : TenantProduct()
//}