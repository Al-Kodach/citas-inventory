package com.citacomsys.module.inventory.request

import com.citacomsys.types.Money
import kotlin.time.Instant

data class SellRequest(
    val organizationId: String,
    val productId: String,
    val quantity: Int,
    val saleId: String,
    val actorId: String,
    val actorName: String
)

data class SellResult(
    val items: List<SoldItem>,
    val totalAmount: Money,
    val totalQuantity: Int
)

data class SoldItem(
    val productId: String,
    val productName: String,
    val batchNumber: String?,
    val expiryDate: Instant?,
    val batchId: String,
    val quantity: Int,
    val soldUnitPrice: Money,
    val expectedUnitPrice: Money,
) {
    val totalPrice: Money
        get() = soldUnitPrice * quantity
}