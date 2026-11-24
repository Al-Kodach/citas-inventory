package com.citacomsys.api.platform.repository

import com.citacomsys.api.platform.model.ProductPackageDetails

interface ProductPackageRepository {
    suspend fun getAllPublic(): List<ProductPackageDetails>
    suspend fun getPackageDetails(packageId: String): Result<ProductPackageDetails>
    suspend fun save(details: ProductPackageDetails): Result<ProductPackageDetails>
}