package com.citacomsys.client.customer.data

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.core.domain.repository.GlobalSearchProvider
import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.remote.isErrorRetriable
import com.citacomsys.module.api.customer.*
import com.citacomsys.module.api.customer.dao.CustomerLocalDao
import com.citacomsys.shared.validation.Validators
import com.citacomsys.shared.validation.validate
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.EmailAddress
import com.citacomsys.types.EntityType
import com.citacomsys.types.Money
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.generateUUID
import io.github.aakira.napier.Napier
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

internal class ClientCustomerRepository(
    private val customerService: CustomerService,
    private val localDao: CustomerLocalDao,
    private val dbManager: InventoryDatabaseManager,
    private val sessionManager: AppSessionManager,
    private val json: Json
) : CustomerRepository, GlobalSearchProvider {

    override suspend fun getOrCreateCustomer(
        request: CustomerRegistrationRequest,
        workspaceId: String
    ): CustomerItem {
        val phone = request.phone?.ifBlank { null }?.let { PhoneNumber(it) }
        val email = request.email?.ifBlank { null }?.let {
            validate(it, Validators.email()).errorMessage?.let { error(it) }
            it
        }

        val match = phone?.let { localDao.findItemByPhone(phone.value) }
            ?: email?.let { localDao.findItemByEmail(it) }

        if (match != null) {
            Napier.d { "Customer found" }
            return match
        }

        val customerId = generateUUID()

        val customer = Customer(
            id = customerId,
            workspaceId = request.workspaceId,
            branchId = request.branchId,
            fullName = request.fullName,
            phone = request.phone,
            email = request.email,
            isAlertsConsented = request.isConsented,
            createdAt = request.createdAt,
            updatedAt = request.createdAt
        )

        val item = CustomerItem(
            id = customerId,
            workspaceId = request.workspaceId,
            branchId = request.branchId,
            fullName = request.fullName,
            phone = request.phone,
            email = request.email,
            timestamp = request.createdAt
        )

        if (sessionManager.isSubscribed(workspaceId).not()) {
            localDao.save(customer)
            Napier.d { "Offline deployed workspace, Local only" }
            return item
        }

        return try {
            customerService.getOrCreateCustomer(request)
            localDao.save(customer)
            item
        } catch (e: Exception) {
            Napier.e(e) { "Customer creation failed" }

            if (isErrorRetriable(e)) {
                Napier.i("Enqueue customer create task")
                dbManager.transaction {
                    dbManager.getDatabase().pendingRequestQueries.insertRequest(
                        id = generateUUID(),
                        payload = null,
                        workspace_id = request.workspaceId,
                        endpoint = "api/v1/customers",
                        method = RequestMethod.POST,
                        entityType = EntityType.CUSTOMER,
                        entity_id = customerId,
                        dependant_id = null,
                        eventAction = "CREATE_CUSTOMER",
                        actor_id = request.actorId,
                        actor_name = request.actorName,
                        branch_id = request.branchId,
                        routing_chain_json = ActiveRoutingChainHolder.get(),
                        createdAt = Clock.System.now().toString()
                    )
                }
                item
            } else throw e
        }
    }

    override fun observeLedger(limit: Long): Flow<List<CustomerLedgerEntry>> =
        localDao.observeLedger(limit)

    override suspend fun findById(customerId: String, workspaceId: String): Result<Customer> {
        val local = localDao.findById(customerId)

        if (local != null) {
            return Result.success(local)
        }

        if (sessionManager.isSubscribed(workspaceId).not()) error("Customer not found")
        return runCatching {
            customerService.findById(
                customerId,
                workspaceId
            ) ?: error("Customer not found")
        }
    }

    override suspend fun findItemById(
        customerId: String,
        workspaceId: String
    ): Result<CustomerItem> = runCatching {
        localDao.findItemById(customerId) ?: throw NotFoundException("Customer not found")
    }

    override fun search(query: String): Flow<List<SearchResult>> = flow {
        val query = PageQuery(
            search = query.trim()
        )

        val list = localDao.search(query).firstOrNull() ?: run {
            if (sessionManager.isSubscribed().not()) return@run emptyList()
            val result = customerService.fetchPaged(sessionManager.organizationId!!, query)
            val items = result.items
            localDao.save(items)
            items
        }

        emit(
            list.map { customer ->
                SearchResult(
                    id = customer.id,
                    title = customer.fullName,
                    subtitle = "Customer • ${customer.phone ?: "No Phone"}",
                    type = EntityType.CUSTOMER,
                    metadata = emptyMap()
                )
            }
        )
    }

    override suspend fun delete(
        customerId: String,
        workspaceId: String,
        actorId: String,
        actorName: String?
    ) {
        localDao.softDeleteLocal(customerId)

        if (sessionManager.isSubscribed(workspaceId).not()) {
            return
        }

        runCatching {
            customerService.delete(workspaceId, customerId)
        }.onFailure {
            dbManager.transaction {
                dbManager.getDatabase().pendingRequestQueries.insertRequest(
                    id = generateUUID(),
                    payload = null,
                    workspace_id = workspaceId,
                    endpoint = "api/v1/customers/$customerId",
                    method = RequestMethod.DELETE,
                    entityType = EntityType.CUSTOMER,
                    entity_id = customerId,
                    dependant_id = null,
                    eventAction = "DELETE_CUSTOMER",
                    actor_id = actorId,
                    actor_name = actorName ?: "",
                    branch_id = "",
                    routing_chain_json = ActiveRoutingChainHolder.get(),
                    createdAt = Clock.System.now().toString()
                )
            }
        }
    }

    override suspend fun recordTransaction(
        customerId: String,
        request: CustomerLedgerRequest,
    ): CustomerLedgerEntry {
        val entry = CustomerLedgerEntry(
            id = generateUUID(),
            customerId = customerId,
            amount = Money(request.amountMinor, request.currency),
            type = request.type,
            method = request.method,
            reference = request.reference,
            recordedAt = Clock.System.now(),
        )

        // Cloud or mother hub
        if (sessionManager.isSubscribed(request.workspaceId).not()) {
            localDao.saveLedgerEntry(customerId, entry)
            return entry
        }

        return try {
            customerService.recordTransaction(customerId, request)
        } catch (_: Exception) {
            localDao.saveLedgerEntry(customerId, entry)

            dbManager.transaction {
                dbManager.getDatabase().pendingRequestQueries.insertRequest(
                    id = generateUUID(),
                    payload = json.encodeToString(
                        CustomerLedgerRequest.serializer(),
                        request
                    ),
                    workspace_id = request.workspaceId,
                    endpoint = "api/v1/customers/$customerId/ledger",
                    method = RequestMethod.POST,
                    entityType = EntityType.CUSTOMER,
                    entity_id = entry.id,
                    dependant_id = customerId,
                    eventAction = "RECORD_TRANSACTION",
                    actor_id = request.actorId,
                    actor_name = request.actorName,
                    branch_id = request.branchId,
                    routing_chain_json = ActiveRoutingChainHolder.get(),
                    createdAt = Clock.System.now().toString()
                )
            }
            entry
        }
    }

    override suspend fun getLedgerForSale(
        saleId: String,
        workspaceId: String
    ): List<CustomerLedgerEntry> {
        if (sessionManager.isSubscribed(workspaceId).not()) {
            return localDao.selectLedgerForSale(saleId).firstOrNull() ?: emptyList()
        }

        return customerService.getLedgerForSale(saleId, workspaceId)
    }

    override suspend fun getChangeList(
        workspaceId: String,
        branchId: String,
        since: Instant?
    ): ChangeListResponse<CustomerItem, String> {
        if (sessionManager.isSubscribed(workspaceId).not()) {
            return localDao.getChangeList(workspaceId, branchId, since)
        }

        return runCatching { customerService.getChangeList(workspaceId, branchId, since) }
            .getOrElse { localDao.getChangeList(workspaceId, branchId, since) }
    }

    override suspend fun updateConsent(
        customerId: String,
        workspaceId: String,
        actorId: String,
        actorName: String,
        consented: Boolean
    ): Boolean {
        val customer = localDao.findById(customerId)

        if (customer != null) {
            localDao.save(customer.copy(isAlertsConsented = consented))
        }

        if (sessionManager.isSubscribed(workspaceId).not()) {
            return true
        }

        val effectiveActorId = actorId.ifBlank { sessionManager.currentUserId.orEmpty() }
        val effectiveActorName = actorName.ifBlank { sessionManager.actorName.orEmpty() }

        return runCatching {
            customerService.updateConsent(workspaceId, customerId, consented)
        }.getOrElse {
            val payload = json.encodeToString(
                ConsentUpdateRequest.serializer(),
                ConsentUpdateRequest(isAlertsConsented = consented)
            )

            dbManager.transaction {
                dbManager.getDatabase().pendingRequestQueries.insertRequest(
                    id = generateUUID(),
                    payload = payload,
                    workspace_id = workspaceId,
                    endpoint = "api/v1/customers/$customerId/alert",
                    method = RequestMethod.PUT,
                    entityType = EntityType.CUSTOMER,
                    entity_id = customerId,
                    dependant_id = null,
                    eventAction = "UPDATE_CONSENT",
                    actor_id = effectiveActorId,
                    actor_name = effectiveActorName,
                    branch_id = sessionManager.resolveWriteBranchId(),
                    routing_chain_json = ActiveRoutingChainHolder.get(),
                    createdAt = Clock.System.now().toString()
                )
            }
            true
        }
    }

    override suspend fun fetchPaged(
        organizationId: String,
        query: PageQuery
    ): PageResult<CustomerItem> {
        return localDao.fetchPagedLocal(query)
    }
}
