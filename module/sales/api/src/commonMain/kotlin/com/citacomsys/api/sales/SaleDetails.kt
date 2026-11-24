package com.citacomsys.api.sales

import com.citacomsys.types.Money
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class SaleDetails(
    val sale: Sale,
    val items: List<Item>,
    val payments: List<Payment>
) {
    @Serializable
    data class Item(
        val productId: String,
        val productName: String,
        val batchId: String,
        val batchNumber: String?,
        val expiryDate: LocalDate?,
        val quantity: Int,
        val unitPrice: Money,
    ) {
        val totalPrice: Money
            get() = unitPrice * quantity
    }
}

