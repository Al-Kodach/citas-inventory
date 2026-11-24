package com.citacomsys.api.platform.remote

import com.citacomsys.api.platform.model.PackageBillingTerm
import com.citacomsys.api.platform.model.ProductPackageDetails

interface ProductPackageService {
    suspend fun getPublicPackages(): List<ProductPackageDetails>
    suspend fun getAvailableUpgrades(workspaceId: String): List<ProductPackageDetails>
    suspend fun getBillingTerms(workspaceId: String): List<PackageBillingTerm>
    suspend fun getPromotionalPackages(workspaceId: String): List<ProductPackageDetails>
}