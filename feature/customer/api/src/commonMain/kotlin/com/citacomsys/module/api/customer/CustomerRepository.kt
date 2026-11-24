package com.citacomsys.module.api.customer

import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface CustomerRepository {
    suspend fun getOrCreateCustomer(
        request: CustomerRegistrationRequest,
        workspaceId: String
    ): CustomerItem

    suspend fun findById(customerId: String, workspaceId: String): Result<Customer>
    suspend fun findItemById(customerId: String, workspaceId: String): Result<CustomerItem>
    fun observeLedger(limit: Long): Flow<List<CustomerLedgerEntry>>
    suspend fun delete(
        customerId: String, 
        workspaceId: String,
        actorId: String = "",
        actorName: String? = null
    )

    suspend fun recordTransaction(
        customerId: String,
        request: CustomerLedgerRequest
    ): CustomerLedgerEntry

    suspend fun getLedgerForSale(
        saleId: String,
        workspaceId: String
    ): List<CustomerLedgerEntry>

    suspend fun getChangeList(
        workspaceId: String,
        branchId: String,
        since: Instant?
    ): ChangeListResponse<CustomerItem, String>

    suspend fun updateConsent(
        customerId: String,
        workspaceId: String,
        actorId: String = "",
        actorName: String = "",
        consented: Boolean
    ): Boolean

    suspend fun fetchPaged(
        organizationId: String,
        query: PageQuery
    ): PageResult<CustomerItem>
}
