package com.citacomsys.api.billing

import com.citacomsys.types.BillingStatus
import com.citacomsys.types.BillingType
import com.citacomsys.types.Money
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
@SerialName("TransactionHistory")
data class TransactionHistory(
    val id: String,
    val workspaceId: String,
    val referenceCode: String,
    val summary: String,
    val type: BillingType,
    val status: BillingStatus,
    val amount: Money,
    val createdAt: Instant,
) {
    val isPaid get() = status == BillingStatus.PAID
    val date get() = createdAt.toString().substringBefore("T")
}


@Serializable
@SerialName("PaymentSubmission")
data class PaymentSubmission(
    val invoiceId: String,
    val transactionId: String?,
    val payerName: String?,
    val submittedAt: Instant = Clock.System.now(),
    val note: String? = null
)
