package com.citacomsys.api.sales

import kotlinx.serialization.Serializable

@Serializable
data class SalesRecord(
    val sale: Sale,
    val items: List<SaleItem>,
    val payments: List<Payment>
)