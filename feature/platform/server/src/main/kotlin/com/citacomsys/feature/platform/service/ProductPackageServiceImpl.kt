package com.citacomsys.feature.platform.service

import com.citacomsys.api.platform.repository.ProductPackageRepository
import com.citacomsys.api.platform.model.PackageBillingTerm
import com.citacomsys.api.platform.repository.PackageBillingTermRepository
import com.citacomsys.api.platform.model.ProductPackageDetails
import com.citacomsys.api.platform.remote.ProductPackageService
import com.citacomsys.api.workspace.repository.WorkspaceResourceUsageRepository

internal class ProductPackageServiceImpl(
    private val packageRepository: ProductPackageRepository,
    private val resourceUsageRepository: WorkspaceResourceUsageRepository,
    private val packageBillingTermRepository: PackageBillingTermRepository
) : ProductPackageService {

    override suspend fun getPublicPackages(): List<ProductPackageDetails> {
        return packageRepository.getAllPublic()
    }

    override suspend fun getAvailableUpgrades(workspaceId: String): List<ProductPackageDetails> {
        val allPackages = packageRepository.getAllPublic()
        val currentPackage = resourceUsageRepository.getWorkspaceResourceAllocation(workspaceId) ?: error("Resource Allocation not found")
        return allPackages.filter { it.packageInfo.tier.ordinal > currentPackage.tier.ordinal }
    }

    override suspend fun getBillingTerms(workspaceId: String): List<PackageBillingTerm> {
        val currentPackage = resourceUsageRepository.getWorkspaceResourceAllocation(workspaceId) ?: error("Resource Allocation not found")

        return packageBillingTermRepository.getTerms(currentPackage.packageId)
    }

    override suspend fun getPromotionalPackages(workspaceId: String): List<ProductPackageDetails> {
        return emptyList()
    }
}
