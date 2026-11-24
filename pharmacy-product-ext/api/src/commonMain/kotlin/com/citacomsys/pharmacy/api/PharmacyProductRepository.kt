package com.citacomsys.pharmacy.api

import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.pharmacy.api.model.PharmacyProduct

interface PharmacyProductRepository: ProductRepository {
    /**
     * READ: Single lookup using the view.
     */
    suspend fun getPharmacyProduct(id: String, workspaceId: String): PharmacyProduct?
    suspend fun updatePharmacyProduct(product: PharmacyProduct, workspaceId: String)

    /**
     * WRITE: Orchestrates generic identity (baseRepo) and clinical metadata (PharmacyProductsTable).
     */
    suspend fun savePharmacyProduct(product: PharmacyProduct)
}
