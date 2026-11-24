package com.citacomsys.api.billing

import com.citacomsys.api.platform.model.AppliedPromotion
import com.citacomsys.types.BillingOrigin
import com.citacomsys.types.BillingReason
import com.citacomsys.types.BillingStatus
import com.citacomsys.types.BillingType
import com.citacomsys.types.Money
import com.citacomsys.types.PriceDiscount
import com.citacomsys.types.PriceTax
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("BillingRecord")
data class BillingRecord(
    val id: String,
    val workspaceId: String,
    val wsName: String,
    val type: BillingType,
    val reason: BillingReason,
    val origin: BillingOrigin,
    val status: BillingStatus,
    val quote: BillingQuote,
    val subscriptionVersion: Long?,
    val referenceCode: String,
    val paymentTarget: String,
    val paymentRecipientName: String,
    val paymentChannel: PaymentChannel,
    val amount: Money,
    val summary: String,
    val createdBy: String,

    val verifiedBy: String? = null,
    val paymentSubmission: PaymentSubmission? = null,
    val fulfillmentAttempts: Int = 0,
    val lastFulfillmentError: String? = null,
    val fulfilledAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant?,
) {

    val canCancelInvoice: Boolean
        get() = origin == BillingOrigin.USER_REQUEST && !status.isTerminal()
}

@Serializable
@SerialName("BillingQuote")
data class BillingQuote(
    val type: BillingType,
    val title: String,
    val description: String,
    val payload: BillingPayload,
    val subtotal: Money,
    val total: Money,
    val discounts: List<PriceDiscount> = emptyList(),
    val taxes: List<PriceTax> = emptyList(),
    val appliedPromotions: List<AppliedPromotion>,
    val promotionExpiresAt: Instant? = null,
    val generatedAt: Instant
)


enum class PaymentChannel {
    MOBILE_MONEY
}


@Serializable
@SerialName("BillInvoice")
data class BillInvoice(
    val id: String,
    val workspaceId: String,
    val tenantName: String,
    val paymentTarget: String,
    val paymentRecipientName: String,
    val type: BillingType,
    val status: BillingStatus,
    val origin: BillingOrigin,
    val amount: Money,
    val referenceCode: String,
    val paymentChannel: PaymentChannel,
    val summary: String,
)


@Serializable
@SerialName("BillingSettings")
data class BillingSettings(
    val renewalWindowDays: Int = 14,
    val maintenanceIntervalHours: Int = 6
)
