package com.citacomsys.module.server.customer

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.query.queryPage
import com.citacomsys.module.api.customer.*
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.module.server.customer.exposedTable.CustomerTable
import com.citacomsys.module.server.customer.exposedTable.CustomerLedgerTable
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.EmailAddress
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.Money
import com.citacomsys.types.PhoneNumber
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

internal class CustomerServiceImpl : CustomerService {

    override suspend fun getOrCreateCustomer(
        request: CustomerRegistrationRequest,
        organizationId: String
    ): CustomerItem = dbQuery {
        val workspaceUuid = UUID.fromString(request.workspaceId)
        val branchUuid = UUID.fromString(request.branchId)
        val workspaceId = request.workspaceId
        val branchId = request.branchId

        // Attempt lookup based on branch phone number identity index (if phone is provided)
        if (request.phone != null) {
            val existingRow = CustomerTable.selectAll()
                .where {
                    (CustomerTable.branchId eq branchUuid) and
                            (CustomerTable.phone eq request.phone) and
                            (CustomerTable.organizationId eq organizationId.toUuid())
                }
                .singleOrNull()

            if (existingRow != null) {
                return@dbQuery existingRow.toItem()
            }
        }

        // Not found -> Safe creation
        val newId = UUID.randomUUID()
        CustomerTable.insert {
            it[CustomerTable.id] = newId
            it[CustomerTable.organizationId] = organizationId.toUuid()
            it[CustomerTable.workspaceId] = workspaceUuid
            it[CustomerTable.branchId] = branchUuid
            it[CustomerTable.fullName] = request.fullName
            it[CustomerTable.phone] = request.phone
            it[CustomerTable.email] = request.email
            it[CustomerTable.creditLimitAmount] = request.creditLimitMinor
            it[CustomerTable.creditLimitCurrency] = "GHS"
            it[CustomerTable.isAlertsConsented] = request.isConsented
            it[CustomerTable.createdAt] = request.createdAt
            it[CustomerTable.updatedAt] = request.createdAt
        }

        CustomerItem(
            id = newId.toString(),
            workspaceId = workspaceId,
            branchId = branchId,
            fullName = request.fullName,
            phone = request.phone,
            email = request.email,
            timestamp = request.createdAt
        )
    }

    override suspend fun findById(
        customerId: String,
        workspaceId: String
    ): Customer? = dbQuery {
        val customerUuid =
            runCatching { UUID.fromString(customerId) }.getOrNull() ?: return@dbQuery null

        val q = CustomerTable.selectAll()
            .where {
                (CustomerTable.id eq customerUuid) and
                        (CustomerTable.workspaceId eq workspaceId.toUuid())
            }

        q.map { it.toProfile(fetchLedger = true) }.singleOrNull()
    }

    override suspend fun delete(workspaceId: String, customerId: String) {
        val customer = findById(customerId, workspaceId) ?: return
        dbQuery {
            CustomerTable.update({
                (CustomerTable.id eq UUID.fromString(customerId)) and
                        (CustomerTable.workspaceId eq UUID.fromString(workspaceId))
            }) {
                it[fullName] = "Deleted Customer"
                it[phone] = customer.phone?.let { n -> PhoneNumber(n).mask }
                it[email] = customer.email?.let { e -> EmailAddress(e).getMaskedEmail() }
                it[isAlertsConsented] = false
                it[deletedAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun getChangeList(
        workspaceId: String,
        branchId: String,
        since: Instant?
    ): ChangeListResponse<CustomerItem, String> = dbQuery {
        val cutoff = since ?: Instant.fromEpochMilliseconds(0)

        val query = CustomerTable.selectAll()
            .where { CustomerTable.workspaceId eq workspaceId.toUuid() }

        if (!branchId.isNullOrBlank()) {
            query.andWhere { CustomerTable.branchId eq branchId.toUuid() }
        }
        query.andWhere { CustomerTable.updatedAt greater cutoff }

        val rows = query.toList()
        val (deletedRows, activeRows) = rows.partition { it[CustomerTable.deletedAt] != null }

        ChangeListResponse(
            updated = activeRows.map { it.toItem() },
            deleted = deletedRows.map { it[CustomerTable.id].value.toString() }
        )
    }

    override suspend fun updateConsent(
        workspaceId: String,
        customerId: String,
        isConsented: Boolean
    ): Boolean = dbQuery {
        CustomerTable.update({
            (CustomerTable.id eq UUID.fromString(customerId)) and
                    (CustomerTable.workspaceId eq UUID.fromString(workspaceId))
        }) {
            it[isAlertsConsented] = isConsented
            it[updatedAt] = Clock.System.now()
        } > 0
    }

    override suspend fun fetchPaged(
        organizationId: String,
        query: PageQuery
    ): PageResult<CustomerItem> {
        val branchUuid = query.branchId?.let { UUID.fromString(it) }

        return CustomerTable.queryPage(
            pageQuery = query,
            searchColumns = listOf(
                CustomerTable.fullName,
                CustomerTable.phone,
                CustomerTable.email
            ),
            filterBuilder = {
                andWhere { CustomerTable.organizationId eq organizationId.toUuid() }
                if (query.workspaceId != null) {
                    andWhere { CustomerTable.workspaceId eq query.workspaceId!!.toUuid() }
                }
                if (branchUuid != null) {
                    andWhere { CustomerTable.branchId eq branchUuid }
                }
                this
            },
            defaultSort = CustomerTable.fullName,
            mapper = { it.toItem() }
        )
    }

    override suspend fun recordTransaction(
        customerId: String,
        request: CustomerLedgerRequest
    ): CustomerLedgerEntry = dbQuery {
        val workspaceId = request.workspaceId
        val customerId = customerId

        val customerUuid = UUID.fromString(customerId)
        val workspaceUuid = UUID.fromString(workspaceId)
        val now = System.currentTimeMillis()

        // Verify customer exists in workspace
        val customer = CustomerTable.selectAll()
            .where {
                (CustomerTable.id eq customerUuid) and
                        (CustomerTable.workspaceId eq workspaceUuid) and
                        (CustomerTable.branchId eq request.branchId.toUuid())
            }
            .singleOrNull() ?: throw IllegalArgumentException("Customer not found")

        // Insert ledger entry
        val entryId = UUID.randomUUID()
        CustomerLedgerTable.insert {
            it[id] = entryId
            it[this.customerId] = customerUuid
            it[amountMinor] = request.amountMinor
            it[currency] = request.currency
            it[type] = request.type.name
            it[method] = request.method
            it[saleId] = request.saleId?.let { UUID.fromString(it) }
            it[reference] = request.reference
            it[recordedAt] = now
            it[note] = request.note
        }

        // Update customer balance
        val currentBalance = customer[CustomerTable.currentBalanceAmount] ?: 0L
        val balanceChange = when (request.type) {
            CustomerTransactionType.PAYMENT, CustomerTransactionType.REFUND -> -request.amountMinor
            CustomerTransactionType.CREDIT_PURCHASE, CustomerTransactionType.ADJUSTMENT -> request.amountMinor
        }
        val newBalance = currentBalance + balanceChange

        CustomerTable.update({ CustomerTable.id eq customerUuid }) {
            it[currentBalanceAmount] = newBalance
            it[currentBalanceCurrency] = request.currency
            it[updatedAt] = Clock.System.now()
        }

        CustomerLedgerEntry(
            id = entryId.toString(),
            customerId = customerId,
            amount = Money(request.amountMinor, request.currency),
            type = request.type,
            method = request.method,
            saleId = request.saleId,
            reference = request.reference,
            recordedAt = Instant.fromEpochMilliseconds(now),
            note = request.note
        )
    }

    override suspend fun getLedgerForSale(
        saleId: String,
        workspaceId: String
    ): List<CustomerLedgerEntry> {
        return dbQuery {
            CustomerLedgerTable.selectAll()
                .where { CustomerLedgerTable.saleId eq UUID.fromString(saleId) }
                .map { it.toLedger() }
        }
    }


    private fun ResultRow.toProfile(fetchLedger: Boolean = false): Customer {
        val id = this[CustomerTable.id].value
        val creditLimit = if (this[CustomerTable.creditLimitAmount] != null) {
            Money(
                amountMinor = this[CustomerTable.creditLimitAmount]!!,
                currencyCode = this[CustomerTable.creditLimitCurrency] ?: "GHS"
            )
        } else null

        val currentBalance = if (this[CustomerTable.currentBalanceAmount] != null) {
            Money(
                amountMinor = this[CustomerTable.currentBalanceAmount]!!,
                currencyCode = this[CustomerTable.currentBalanceCurrency] ?: "GHS"
            )
        } else null

        val ledger = if (fetchLedger) {
            CustomerLedgerTable.selectAll()
                .where { CustomerLedgerTable.customerId eq id }
                .orderBy(CustomerLedgerTable.recordedAt, SortOrder.DESC)
                .map { row -> row.toLedger() }
        } else emptyList()

        return Customer(
            id = id.toString(),
            workspaceId = this[CustomerTable.workspaceId].toString(),
            branchId = this[CustomerTable.branchId].toString(),
            fullName = this[CustomerTable.fullName],
            phone = this[CustomerTable.phone],
            email = this[CustomerTable.email],
            isAlertsConsented = this[CustomerTable.isAlertsConsented],
            creditLimit = creditLimit,
            currentBalance = currentBalance,
            ledgerEntries = ledger,
            createdAt = this[CustomerTable.createdAt],
            updatedAt = this[CustomerTable.updatedAt]
        )
    }

    private fun ResultRow.toLedger(): CustomerLedgerEntry {
        return CustomerLedgerEntry(
            id = this[CustomerLedgerTable.id].value.toString(),
            customerId = this[CustomerLedgerTable.customerId].toString(),
            amount = Money(
                amountMinor = this[CustomerLedgerTable.amountMinor],
                currencyCode = this[CustomerLedgerTable.currency]
            ),
            saleId = this[CustomerLedgerTable.saleId]?.toString(),
            type = CustomerTransactionType.valueOf(this[CustomerLedgerTable.type]),
            method = this[CustomerLedgerTable.method],
            reference = this[CustomerLedgerTable.reference],
            recordedAt = Instant.fromEpochMilliseconds(this[CustomerLedgerTable.recordedAt]),
            note = this[CustomerLedgerTable.note]
        )
    }

    private fun ResultRow.toItem() = CustomerItem(
        id = this[CustomerTable.id].value.toString(),
        workspaceId = this[CustomerTable.workspaceId].toString(),
        branchId = this[CustomerTable.branchId].toString(),
        fullName = this[CustomerTable.fullName],
        phone = this[CustomerTable.phone],
        email = this[CustomerTable.email],
        timestamp = this[CustomerTable.updatedAt]
    )
}
