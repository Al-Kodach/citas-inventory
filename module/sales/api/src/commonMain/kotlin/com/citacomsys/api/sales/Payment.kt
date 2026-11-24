package com.citacomsys.api.sales

import com.citacomsys.types.Money
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Payment(
    val id: String,

    val saleId: String,

    val amount: Money,
    val method: PaymentMethod,

    val reference: String?,

    val createdAt: Instant
)