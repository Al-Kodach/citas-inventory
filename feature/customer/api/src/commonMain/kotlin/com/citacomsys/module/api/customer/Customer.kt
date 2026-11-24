package com.citacomsys.module.api.customer

import com.citacomsys.types.Money
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CustomerTransactionType {
    PAYMENT,   // Customer paid money to reduce balance
    CREDIT_PURCHASE, // Customer bought items on credit (increases balance/debt)
    REFUND,    // Money returned to customer
    ADJUSTMENT // Manual correction
}

@Serializable
@SerialName("CustomerLedgerEntry")
data class CustomerLedgerEntry(
    val id: String,
    val customerId: String,
    val amount: Money,
    val type: CustomerTransactionType,
    val method: String? = null, // CASH, MOMO, etc.
    val saleId: String? = null, // Direct link to POS Sale ID
    val reference: String? = null, // Gateway TxID / Check #
    val recordedAt: Instant,
    val note: String? = null
)

@Serializable
@SerialName("CustomerItem")
data class CustomerItem(
    val id: String,
    val fullName: String,
    val workspaceId: String,
    val branchId: String,
    val email: String? = null,
    val phone: String? = null,
    val timestamp: Instant,
)

@Serializable
@SerialName("Customer")
data class Customer(
    val id: String,
    val workspaceId: String,
    val branchId: String,
    val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val isAlertsConsented: Boolean = true,

    // Financial Core Extension
    val creditLimit: Money? = null,
    val currentBalance: Money? = null, // Negative means credit, Positive means debt
    val ledgerEntries: List<CustomerLedgerEntry> = emptyList(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = createdAt
)
