package com.citacomsys.module.api.customer.dao

import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.module.api.customer.Customer
import com.citacomsys.module.api.customer.CustomerLedgerEntry
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Interface for local persistence and reactive observation of customer data.
 */
interface CustomerLocalDao {

    /**
     * Inserts a raw local entry (newly created offline).
     */
    suspend fun save(customer: Customer)
    suspend fun save(item: CustomerItem)
    suspend fun save(items: List<CustomerItem>)

    fun search(query: PageQuery): Flow<List<CustomerItem>>
    suspend fun fetchPagedLocal(query: PageQuery): PageResult<CustomerItem>

    suspend fun deleteById(id: String)
    suspend fun softDeleteLocal(customerId: String)
    suspend fun getChangeList(
        workspaceId: String,
        branchId: String?,
        since: Instant?
    ): ChangeListResponse<CustomerItem, String>

    suspend fun findById(id: String): Customer?
    suspend fun findItemById(id: String): CustomerItem?
    suspend fun findItemByPhone(phone: String): CustomerItem?
    suspend fun findItemByEmail(email: String): CustomerItem?

    suspend fun saveLedgerEntry(customerId: String, entry: CustomerLedgerEntry)

    /**
     * Fetches all ledger entries for a specific customer from the local cache.
     */
    suspend fun getLedgerEntries(customerId: String): List<CustomerLedgerEntry>
    fun selectLedgerForSale(saleId: String): Flow<List<CustomerLedgerEntry>>

    suspend fun maxUpdateAt(workspaceId: String, branchId: String): Instant?
    suspend fun delete(customerId: String)
    fun observeLedger(limit: Long): Flow<List<CustomerLedgerEntry>>
}
