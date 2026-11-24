package com.citacomsys.feature.platform.service

import com.citacomsys.api.platform.model.PackageBillingTerm
import com.citacomsys.api.platform.model.ProductPackageDetails
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.model.ProvisionedPackage
import com.citacomsys.api.platform.repository.PackageBillingTermRepository
import com.citacomsys.api.platform.repository.ProductPackageRepository
import com.citacomsys.api.platform.remote.PackageProvisioningService
import com.citacomsys.shared.displayName
import com.citacomsys.types.PriceLineItem
import com.citacomsys.types.ResourceType

internal class PackageProvisioningServiceImpl(
    private val packageRepository: ProductPackageRepository,
    private val billingTermRepository: PackageBillingTermRepository
) : PackageProvisioningService {

    private val logger = mu.KotlinLogging.logger { }

    override suspend fun provision(request: PkgProvisionRequest): Result<ProvisionedPackage> =
        runCatching {
            // Hydrate full aggregate package configurations directly from database repositories
            val pkgDetails: ProductPackageDetails =
                packageRepository.getPackageDetails(request.packageId).getOrNull()
                    ?: throw IllegalArgumentException("Requested product package profile not found: ${request.packageId}")

            val billingTerm: PackageBillingTerm =
                (if (request.billTermId.isNotBlank()) billingTermRepository.getById(request.billTermId) else null)
                    ?: pkgDetails.billingTerms.find { it.id == request.billTermId }
                    ?: pkgDetails.billingTerms.firstOrNull { it.isDefault }
                    ?: pkgDetails.billingTerms.firstOrNull()
                    ?: throw IllegalArgumentException("Billing term not found for package: ${request.packageId}")

            val pkg = pkgDetails.packageInfo
            val limits = pkgDetails.limits

            // Map direct static invoice parameters
            val baseInvoiceLineItem = PriceLineItem(
                type = ResourceType.PACKAGE,
                description = "${pkg.name} Tier Plan Subscriptions — ${billingTerm.billingCycle.displayName()}",
                quantity = 1,
                unitPriceAmount = billingTerm.priceAmount,
                totalAmount = billingTerm.priceAmount,
            )

            logger.info { "Successfully compiled plan validation calculations matrix for workspace on package: ${pkg.id}" }

            //  Construct production immutable provisioning package
            ProvisionedPackage(
                packageId = pkg.id,
                packageName = pkg.name,
                tier = pkg.tier,
                deploymentMode = billingTerm.deploymentMode,
                billingCycle = billingTerm.billingCycle,

                // Unpack immutable core package boundary constraints
                maxUsers = limits.maxUsers,
                maxDevices = limits.maxDevice,
                maxBranches = limits.maxBranches,
                maxProductsLimit = limits.maxProductsLimit,
                maxActiveBatchesLimit = limits.maxActiveBatchesLimit,
                historyRetentionMonths = limits.historyRetentionMonths,
                smsQuotaPerCycle = pkg.smsQuotaPerCycle,

                modules = pkgDetails.modules,
                trialDays = billingTerm.trialDays,
                lineItems = listOf(baseInvoiceLineItem),
                calculatedPrice = billingTerm.priceAmount,
                campaignId = billingTerm.campaignId
            )
        }
}