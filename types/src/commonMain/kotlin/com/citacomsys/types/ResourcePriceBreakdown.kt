package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ResourcePriceBreakdown")
data class ResourcePriceBreakdown(
    val lineItems: List<PriceLineItem> = emptyList(),
    val subtotal: Money = Money.zero(),
    val discounts: List<PriceDiscount> = emptyList(), // Only save fix-discount
    val total: Money = Money.zero()
)

@Serializable
@SerialName("PriceLineItem")
data class PriceLineItem(
    val type: ResourceType,
    val description: String,
    val quantity: Int,
    val unitPriceAmount: Money,
    val totalAmount: Money
)

@Serializable
@SerialName("PriceDiscount")
data class PriceDiscount(
    val code: String,
    val description: String,
    val amount: Money
)

@Serializable
@SerialName("PriceTax")
data class PriceTax(
    val code: String,
    val name: String,
    val ratePercent: Double,
    val amount: Money
)
