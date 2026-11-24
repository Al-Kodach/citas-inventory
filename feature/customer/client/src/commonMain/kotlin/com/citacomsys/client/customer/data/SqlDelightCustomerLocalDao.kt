package com.citacomsys.client.customer.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.citacomsys.core.database.DriverFactory
import com.citacomsys.core.database.createCustomerDatabase
import com.citacomsys.customer.database.CustomerDatabase
import com.citacomsys.customer.database.CustomerEntity
import com.citacomsys.customer.database.CustomerLedgerEntity
import com.citacomsys.customer.database.CustomerQueries
import com.citacomsys.customer.database.CustomerSearchEntity
import com.citacomsys.module.api.customer.Customer
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.module.api.customer.CustomerLedgerEntry
import com.citacomsys.module.api.customer.CustomerTransactionType
import com.citacomsys.module.api.customer.dao.CustomerLocalDao
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.EmailAddress
import com.citacomsys.types.Money
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.PhoneNumber
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.getOrCreateSecret
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

class CustomerDatabaseManager(
    private val driverFactory: DriverFactory,
    private val vault: KSafe
) {
    private val _database = MutableStateFlow<CustomerDatabase?>(null)
    private val mutex = Mutex()

    val queries by lazy {
        suspend { getDatabase().customerQueries }
    }

    suspend fun getDatabase(): CustomerDatabase {
        return _database.value ?: mutex.withLock {
            _database.value ?: run {
                Napier.d { "Lazy Initializing Encrypted CustomerDatabase..." }
                try {
                    val secret = vault.getOrCreateSecret("customers.db")
                    createCustomerDatabase(driverFactory, secret).also {
                        _database.value = it
                        Napier.i { "CustomerDatabase initialized successfully." }
                    }
                } catch (e: Exception) {
                    Napier.e(e) { "CRITICAL: Failed to initialize CustomerDatabase. Hardware secret inaccessible." }
                    throw e
                }
            }
        }
    }

    val database: Flow<CustomerDatabase> = _database
        .onStart { getDatabase() }
        .filterNotNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class SqlDelightCustomerLocalDao(
    private val manager: CustomerDatabaseManager,
    private val dispatcher: CoroutineDispatcher,
) : CustomerLocalDao {

    override suspend fun save(item: CustomerItem) {
        withContext(dispatcher) {
            manager.queries().insertOrReplaceSearch(
                customer_id = item.id,
                workspace_id = item.workspaceId,
                branch_id = item.branchId,
                full_name = item.fullName,
                phone = item.phone,
                email = item.email,
                timestamp = item.timestamp
            )
        }
    }

    override suspend fun save(items: List<CustomerItem>) {
        withContext(dispatcher) {
            items.forEach { item ->
                manager.queries().insertOrReplaceSearch(
                    customer_id = item.id,
                    full_name = item.fullName,
                    phone = item.phone,
                    email = item.email,
                    timestamp = item.timestamp,
                    workspace_id = item.workspaceId,
                    branch_id = item.branchId
                )
            }
        }
    }

    override suspend fun save(customer: Customer) {
        val query = manager.queries()
        withContext(dispatcher) {
            query.transaction {

                query.insertOrReplace(
                    id = customer.id,
                    workspace_id = customer.workspaceId,
                    branch_id = customer.branchId,
                    full_name = customer.fullName,
                    phone = customer.phone,
                    email = customer.email,
                    is_alerts_consented = customer.isAlertsConsented,
                    credit_limit_amount = customer.creditLimit?.amountMinor,
                    credit_limit_currency = customer.creditLimit?.currencyCode,
                    current_balance_amount = customer.currentBalance?.amountMinor,
                    current_balance_currency = customer.currentBalance?.currencyCode,
                    created_at = customer.createdAt,
                    updated_at = customer.updatedAt
                )

                query.insertOrReplaceSearch(
                    customer_id = customer.id,
                    full_name = customer.fullName,
                    phone = customer.phone,
                    email = customer.email,
                    timestamp = customer.updatedAt,
                    workspace_id = customer.workspaceId,
                    branch_id = customer.branchId
                )
            }
        }
    }

    override fun search(query: PageQuery): Flow<List<CustomerItem>> =
        manager.database.flatMapLatest { db ->
            db.customerQueries.search(
                workspaceId = query.workspaceId,
                branchId = query.branchId,
                search = query.search ?: "",
                limit = query.size.toLong(),
                offset = query.page.toLong()
            ).asFlow()
                .mapToList(dispatcher)
                .map { it.map { entity -> entity.item() } }
        }

    override suspend fun fetchPagedLocal(query: PageQuery): PageResult<CustomerItem> {
        val zeroBasedPage = if (query.page > 0) query.page - 1 else 0
        val offset = (zeroBasedPage * query.size).toLong()
        val limit = query.size.toLong()

        val wsId = query.workspaceId
        val branchId = query.branchId
        val searchPattern = query.search?.takeIf { it.isNotBlank() }

        return withContext(dispatcher) {
            val db = manager.getDatabase()
            val totalCount = db.customerQueries
                .countSearch(workspaceId = wsId, branchId = branchId, search = searchPattern)
                .executeAsOne()

            val items = db.customerQueries
                .searchPaged(
                    workspaceId = wsId,
                    branchId = branchId,
                    search = searchPattern,
                    limit = limit,
                    offset = offset
                )
                .executeAsList()
                .map { it.item() }

            val totalPages =
                if (totalCount == 0L) 0 else ((totalCount + query.size - 1) / query.size).toInt()

            PageResult(
                items = items,
                page = if (query.page <= 0) 1 else query.page,
                size = query.size,
                totalItems = totalCount,
                totalPages = totalPages,
                isLast = (zeroBasedPage + 1) >= totalPages,
                isFirst = zeroBasedPage == 0
            )
        }
    }

    override suspend fun deleteById(id: String) {
        withContext(dispatcher) {
            manager.queries().deleteLocal(id)
        }
    }

    override suspend fun findItemById(id: String): CustomerItem? {
        return withContext(dispatcher) {
            manager.queries().selectItemById(id).executeAsOneOrNull()?.item()
        }
    }

    override suspend fun findById(id: String): Customer? = withContext(dispatcher) {
        manager.queries().selectById(id).executeAsOneOrNull()
            ?.toCustomer()
    }

    override suspend fun findItemByPhone(phone: String): CustomerItem? = withContext(dispatcher) {
        manager.queries().findByPhone(phone).executeAsOneOrNull()?.item()
    }

    override suspend fun findItemByEmail(email: String): CustomerItem? = withContext(dispatcher) {
        manager.queries().findByEmail(email).executeAsOneOrNull()?.item()
    }

    override suspend fun saveLedgerEntry(customerId: String, entry: CustomerLedgerEntry) {
        withContext(dispatcher) {
            manager.queries().insertLedger(
                CustomerLedgerEntity(
                    id = entry.id,
                    customer_id = customerId,
                    amount_minor = entry.amount.amountMinor,
                    currency = entry.amount.currencyCode,
                    type = entry.type.name,
                    method = entry.method,
                    sale_id = entry.saleId,
                    reference = entry.reference,
                    recorded_at = entry.recordedAt,
                    note = entry.note
                )
            )
        }
    }

    override suspend fun getLedgerEntries(customerId: String): List<CustomerLedgerEntry> =
        withContext(dispatcher) {
            manager.queries().getLedgerByCustomer(customerId)
                .executeAsList()
                .map { it.toDomain() }
        }


    override suspend fun maxUpdateAt(workspaceId: String, branchId: String): Instant? {
        return manager.queries().maxUpdateAt(workspaceId, branchId)
            .executeAsOneOrNull()?.MAX
    }

    override suspend fun delete(customerId: String) {
        withContext(dispatcher) {
            manager.queries().deleteAllForCustomer(customerId)
        }
    }

    override suspend fun softDeleteLocal(customerId: String) {
        val now = Clock.System.now()
        withContext(dispatcher) {
            manager.getDatabase().transaction {
                manager.queries().softDeleteLocal(
                    customerId = customerId,
                    deletedAt = now,
                    updatedAt = now
                )
                manager.queries().deleteItem(customerId)
            }
        }
    }

    override suspend fun getChangeList(
        workspaceId: String,
        branchId: String?,
        since: Instant?
    ): ChangeListResponse<CustomerItem, String> {
        val cutoff = since ?: Instant.fromEpochMilliseconds(0)
        return withContext(dispatcher) {
            val db = manager.getDatabase()
            val rows =
                db.customerQueries.getChangesSince(workspaceId, branchId, cutoff).executeAsList()
            val (deletedRows, activeRows) = rows.partition { it.deleted_at != null }

            ChangeListResponse(
                updated = activeRows.map { it.toItem() },
                deleted = deletedRows.map { it.id }
            )
        }
    }

    private fun CustomerEntity.toItem() = CustomerItem(
        id = id,
        workspaceId = workspace_id,
        branchId = branch_id,
        fullName = full_name,
        phone = phone?.let { PhoneNumber.tryCreate(it)?.mask },
        email = email?.let { EmailAddress(it).getMaskedEmail() },
        timestamp = updated_at
    )

    override fun observeLedger(limit: Long): Flow<List<CustomerLedgerEntry>> {
        return manager.database.flatMapLatest { db ->
            db.customerQueries.selectAllLedger(limit).asFlow().mapToList(dispatcher)
                .map { list ->
                    list.map { it.toDomain() }
                }
        }
    }

    override fun selectLedgerForSale(saleId: String): Flow<List<CustomerLedgerEntry>> =
        manager.database.flatMapLatest { db ->
            db.customerQueries.selectLedgerForSale(saleId).asFlow().mapToList(dispatcher)
                .map { list ->
                    list.map { it.toDomain() }
                }
        }

    private fun CustomerSearchEntity.item() = CustomerItem(
        id = customer_id,
        workspaceId = workspace_id,
        branchId = branch_id,
        fullName = full_name,
        phone = phone?.let { PhoneNumber.tryCreate(it)?.mask },
        email = EmailAddress(email).getMaskedEmail(),
        timestamp = timestamp
    )

    private fun CustomerEntity.toCustomer() = Customer(
        id = id,
        workspaceId = workspace_id,
        branchId = branch_id,
        fullName = full_name,
        phone = phone,
        email = email,
        isAlertsConsented = is_alerts_consented,
        creditLimit = credit_limit_amount?.let {
            Money(it, credit_limit_currency ?: "GHS")
        },
        currentBalance = current_balance_amount?.let {
            Money(
                it,
                current_balance_currency ?: "GHS"
            )
        },
        createdAt = created_at,
        updatedAt = updated_at
    )


    private fun CustomerLedgerEntity.toDomain() = CustomerLedgerEntry(
        id = id,
        customerId = customer_id,
        amount = Money(amount_minor, currency),
        type = CustomerTransactionType.valueOf(type),
        method = method,
        reference = reference,
        recordedAt = recorded_at,
        note = note
    )
}
