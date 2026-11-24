package com.citacomsys.api.platform.model

import com.citacomsys.types.ProductModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("UpgradeOffer")
data class UpgradeOffer(
    val packageId: String,
    val billTermId: String,
    val packageName: String,
    val users: Int,
    val devices: Int,
    val smsQuota: Int,
    val maxProductsLimit: Int,
    val maxActiveBatchesLimit: Int,
    val historyRetentionMonths: Int,
    val price: String, // Formatted: "GHS 50.00 / month"
    val features: List<ProductModule>,
    val description: String
)

