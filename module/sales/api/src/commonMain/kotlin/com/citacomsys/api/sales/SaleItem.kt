package com.citacomsys.api.sales

import com.citacomsys.types.Money
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class SaleItem(
    val id: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val batchId: String,
    val batchNumber: String?,
    val expiryDate: LocalDate?,
    val quantity: Int,
    val soldUnitPrice: Money,
    val priceVariance: Money = Money.zero()
) {
    val totalPrice: Money
        get() = soldUnitPrice * quantity
}
