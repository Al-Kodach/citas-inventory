package com.citacomsys.api.sales.dto

import com.citacomsys.types.Money
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class PreviewSaleRequest(
    val workspaceId: String,
    val branchId: String,
    val items: List<SaleItemRequest>
)

@Serializable
data class SalePreview(
    val quoteId: String,
    val totalAmount: Money,
    val items: List<SaleItemSnapshot>,
    val expiresAt: Instant
)

@Serializable
sealed class PreviewResponse {
    @Serializable
    data class Success(val salePreview: SalePreview) : PreviewResponse()

    @Serializable
    data class Error(
        val message: String,
        val details: Map<String, String>
    ) : PreviewResponse()
}
