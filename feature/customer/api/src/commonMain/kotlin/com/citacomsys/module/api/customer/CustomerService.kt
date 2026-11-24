package com.citacomsys.module.api.customer

import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
@SerialName("register")
data class CustomerRegistrationRequest(
    val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val creditLimitMinor: Long? = null,
    val branchId: String,
    val workspaceId: String,
    val actorId: String,
    val actorName: String,
    val isConsented: Boolean,
    val createdAt: Instant = Clock.System.now()
)

@Serializable
@SerialName("alert")
data class ConsentUpdateRequest(
    val isAlertsConsented: Boolean
)

@Serializable
@SerialName("CustomerLedgerRequest")
data class CustomerLedgerRequest(
    val workspaceId: String,
    val branchId: String,
    val amountMinor: Long,
    val currency: String,
    val type: CustomerTransactionType,
    val method: String? = null,
    val saleId: String? = null,
    val reference: String? = null,
    val note: String? = null,
    val actorId: String,
    val actorName: String,
)

interface CustomerService {
    suspend fun getOrCreateCustomer(
        request: CustomerRegistrationRequest,
        organizationId: String = "",
    ): CustomerItem

    suspend fun findById(
        customerId: String,
        workspaceId: String = "",
    ): Customer?

    suspend fun getChangeList(
        workspaceId: String,
        branchId: String,
        since: Instant?
    ): ChangeListResponse<CustomerItem, String>

    suspend fun delete(workspaceId: String, customerId: String)

    suspend fun updateConsent(
        workspaceId: String,
        customerId: String,
        isConsented: Boolean
    ): Boolean

    suspend fun fetchPaged(
        organizationId: String = "",
        query: PageQuery,
    ): PageResult<CustomerItem>

    suspend fun recordTransaction(
        customerId: String,
        request: CustomerLedgerRequest
    ): CustomerLedgerEntry

    suspend fun getLedgerForSale(saleId: String, workspaceId: String): List<CustomerLedgerEntry>
}
