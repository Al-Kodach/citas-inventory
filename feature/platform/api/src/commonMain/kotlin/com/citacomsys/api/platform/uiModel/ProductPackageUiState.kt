package com.citacomsys.api.platform.uiModel

import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.ProductModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ProductPackageUiState")
data class ProductPackageUiState(
    val id: String,
    val name: String,
    val description: String?,
    val recommended: Boolean,
    val deploymentMode: DeploymentMode,

    // Counter-Based Constraints
    val maxUsers: Int,
    val maxDevices: Int,
    val maxBranches: Int,
    val maxProductsLimit: Int,
    val maxActiveBatchesLimit: Int,
    val smsQuotaPerCycle: Int,

    // Retention Policy Bracketeer
    val historyRetentionMonths: Int,

    val supportedModules: Set<ProductModule>,
    val billingTerms: List<BillTermUiState>,
    val note: String? = null
)

@Serializable
data class BillTermUiState(
    val id: String,
    val packageId: String,
    val deploymentMode: DeploymentMode,
    val billingCycleName: String,
    val originalPrice: String,
    val promoPrice: String?,
    val pricePerMonth: String,
    val automatedSavingsPercent: Int? = null,
    val savings: String?,
    val trialDays: Int,
    val isDefault: Boolean,
    val hasPromo: Boolean,
    val campaignId: String,
    val campaignCode: String,
    val remainingDaysToEnd: Int?,
    val effects: List<PromotionUiState>
)

@Serializable
data class SmsPackageUiState(
    val id: String,
    val units: Int,
    val originalPrice: String,
    val promoPrice: String?,
    val savings: String?,
    val hasPromo: Boolean,
    val campaignId: String,
    val campaignCode: String,
    val remainingDaysToEnd: Int,
    val effects: List<PromotionUiState>
)



@Serializable
@SerialName("PromotionUiState")
data class PromotionUiState(
    val name: String,
    val description: String,
    val temporary: Boolean
)