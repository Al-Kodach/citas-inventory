package com.citacomsys.shared

import com.citacomsys.types.Money
import kotlin.time.Instant

data class Receipt(
    val id: String,
    val pharmacyName: String,

    val saleId: String,
    val issuedAt: Instant,
    val cashierName: String?,

    val items: List<Item>,

    val subtotal: Money,
    val total: Money,

    val payments: List<PaymentLine>
) {
    data class Item(
        val name: String,
        val batchNumber: String?,
        val quantity: Int,
        val unitPrice: Money,
        val totalPrice: Money
    )

    data class PaymentLine(
        val method: String,
        val amount: Money
    )
}