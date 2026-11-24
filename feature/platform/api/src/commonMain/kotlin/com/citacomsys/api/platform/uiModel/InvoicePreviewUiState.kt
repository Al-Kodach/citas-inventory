package com.citacomsys.api.platform.uiModel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("PreviewRequest")
sealed class PreviewRequest {
    @Serializable
    @SerialName("Sms")
    data class SmsTopup(val pkgId: String) : PreviewRequest()

    @Serializable
    @SerialName("Subscription")
    data class Subscription(
        val packageId: String,
        val billTermId: String
    ) : PreviewRequest()
}

@Serializable
@SerialName("InvoicePreviewUiState")
data class InvoicePreviewUiState(
    val title: String = "Package Summary",
    val lineItems: List<InvoiceLineUiState> = emptyList(),
    val discounts: List<DiscountUiState> = emptyList(),
    val taxes: List<TaxUiState> = emptyList(),
    val subtotal: String = "",
    val total: String = "",
    val validUntil: Instant? = null,
    val footerNote: String? = null
)

@Serializable
data class InvoiceLineUiState(
    val description: String,
    val quantity: Int,
    val unitPrice: String,
    val totalAmount: String
)

@Serializable
data class DiscountUiState(
    val description: String,
    val amount: String
)

@Serializable
data class TaxUiState(
    val description: String,
    val amount: String
)
