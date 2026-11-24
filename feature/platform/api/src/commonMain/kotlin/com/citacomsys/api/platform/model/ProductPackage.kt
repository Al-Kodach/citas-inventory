package com.citacomsys.api.platform.model

import com.citacomsys.types.BillingCycle
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.Money
import com.citacomsys.types.PackageTier
import com.citacomsys.types.PriceLineItem
import com.citacomsys.types.ProductModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
@SerialName("ProductPackage")
data class ProductPackage(
    val id: String,
    val name: String,
    val tier: PackageTier,
    val smsQuotaPerCycle: Int = 0,
    val description: String? = null,
    val recommended: Boolean = false,
    val isPublic: Boolean = true,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)

@Serializable
@SerialName("PackageBillingTerm")
data class PackageBillingTerm(
    val id: String,
    val packageId: String,
    val deploymentMode: DeploymentMode,
    val billingCycle: BillingCycle,
    val priceAmount: Money,
    val campaignId: String?,
    val trialDays: Int = 14,
    val isDefault: Boolean = false,
    val updatedAt: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now()
)

@Serializable
@SerialName("PackageLimits")
data class PackageLimits(
    val packageId: String,
    val maxUsers: Int,
    val maxDevice: Int,
    val maxBranches: Int,
    val maxProductsLimit: Int,
    val maxActiveBatchesLimit: Int,
    val historyRetentionMonths: Int
)

@Serializable
@SerialName("ProvisionedPackage")
data class ProvisionedPackage(
    val packageId: String,
    val packageName: String,
    val tier: PackageTier,
    val deploymentMode: DeploymentMode,
    val billingCycle: BillingCycle,

    // Core Limits & Rules Carried Over
    val maxUsers: Int,
    val maxDevices: Int,
    val maxBranches: Int,
    val maxProductsLimit: Int,
    val maxActiveBatchesLimit: Int,
    val historyRetentionMonths: Int,
    val smsQuotaPerCycle: Int,

    // Active System Configurations
    val modules: Set<ProductModule>,
    val trialDays: Int,
    val lineItems: List<PriceLineItem>,
    val calculatedPrice: Money,
    val campaignId: String?
)

@Serializable
@SerialName("PkgProvisionRequest")
data class PkgProvisionRequest(
    val packageId: String,
    val billTermId: String
)


@SerialName("ProductPackageDetails")
@Serializable
data class ProductPackageDetails(
    val packageInfo: ProductPackage,
    val limits: PackageLimits,
    val billingTerms: List<PackageBillingTerm>,
    val modules: Set<ProductModule>
)

@Serializable
@SerialName("SmsPackage")
data class SmsPackage(
    val id: String,
    val units: Int,
    val price: Money,
    val campaignId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
