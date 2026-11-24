package com.citacomsys.api.sales.dto

import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.types.Money
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CreateSaleRequest(
    val id: String,
    val workspaceId: String,
    val branchId: String,
    val customerId: String? = null,
    val quoteId: String? = null,
    val items: List<SaleItemRequest>,
    val payments: List<PaymentRequest>,
    val initialDeposit: PaymentRequest? = null,
    val actorId: String,
    val actorName: String,
    val createdBy: String = "",
    val createdAt: Instant? = null
)

@Serializable
data class PaymentRequest(
    val amount: Money,
    val method: PaymentMethod,
    val reference: String? = null
)

@Serializable
data class SaleItemRequest(
    val productId: String,
    val quantity: Int,
    /**
     * The unit price at which the item was sold on the client.
     * Used to detect discrepancies during ingestion.
     */
    val cachedUnitPrice: Money? = null
)
