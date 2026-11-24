package com.citacomsys.retail.api

import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.retail.api.model.RetailProduct

interface RetailProductRepository : ProductRepository {
    suspend fun getRetailProduct(id: String, workspaceId: String): RetailProduct?
    suspend fun saveRetailProduct(product: RetailProduct)
}
