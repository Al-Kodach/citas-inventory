package com.citacomsys.api.sales

import com.citacomsys.types.Money
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Sale(
    val id: String,
    val branchId: String,
    val workspaceId: String,
    val customerId: String? = null,
    val totalAmount: Money,
    val priceVariance: Money,
    val totalItems: Int,
    val status: SaleStatus,
    val createdAt: Instant,
    val createdBy: String
)

enum class SaleStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}

enum class PaymentMethod {
    CASH,
    MOBILE_MONEY,
    CARD,
    CREDIT
}
