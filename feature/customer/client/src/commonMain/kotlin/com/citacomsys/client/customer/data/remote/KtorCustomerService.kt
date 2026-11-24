package com.citacomsys.client.customer.data.remote

import com.citacomsys.core.remote.BaseApi
import com.citacomsys.module.api.customer.ConsentUpdateRequest
import com.citacomsys.module.api.customer.CustomerLedgerEntry
import com.citacomsys.module.api.customer.CustomerLedgerRequest
import com.citacomsys.module.api.customer.Customer
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.module.api.customer.CustomerRegistrationRequest
import com.citacomsys.module.api.customer.CustomerService
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlin.time.Instant

internal class KtorCustomerService(
    client: HttpClient,
    private val endpoint: String = "api/v1/customers"
) : CustomerService, BaseApi(client) {

    override suspend fun getOrCreateCustomer(
        request: CustomerRegistrationRequest,
        organizationId: String
    ): CustomerItem = executeRequest {
        client.post(endpoint) {
            setBody(request)
        }
    }

    override suspend fun findById(
        customerId: String,
        workspaceId: String
    ): Customer? = executeRequest {
        client.get("$endpoint/$customerId") {
            url {
                parameters.append("wsId", workspaceId)
            }
        }
    }

    override suspend fun getChangeList(
        workspaceId: String,
        branchId: String,
        since: Instant?
    ): ChangeListResponse<CustomerItem, String> {
        return executeRequest {
            client.get("$endpoint/change-list") {
                url {
                    parameters.append("wsId", workspaceId)
                    parameters.append("branchId", branchId)
                    since?.let { parameters.append("since", since.toString()) }
                }
            }
        }
    }

    override suspend fun delete(workspaceId: String, customerId: String) {
        return executeRequest {
            client.delete("$endpoint/$customerId") {
                url {
                    parameters.append("wsId", workspaceId)
                }
            }
        }
    }

    override suspend fun updateConsent(
        workspaceId: String,
        customerId: String,
        isConsented: Boolean
    ): Boolean = executeRequest {
        val request = ConsentUpdateRequest(isConsented)
        client.put("$endpoint/$customerId/consent") {
            setBody(
                mapOf(
                    "request" to request,
                    "wsId" to workspaceId
                )
            )
        }
    }

    override suspend fun fetchPaged(
        organizationId: String,
        query: PageQuery
    ): PageResult<CustomerItem> {
        return executeRequest {
            client.post("$endpoint/query") {
                setBody(query)
            }
        }
    }

    override suspend fun recordTransaction(
        customerId: String,
        request: CustomerLedgerRequest
    ): CustomerLedgerEntry {
        return executeRequest {
            client.post("$endpoint/$customerId/ledger") {
                setBody(request)
            }
        }
    }

    override suspend fun getLedgerForSale(
        saleId: String,
        workspaceId: String
    ): List<CustomerLedgerEntry> {
        return executeRequest {
            client.get("$endpoint/$saleId/ledger") {
                url {
                    parameters.append("wsId", workspaceId)
                }
            }
        }
    }
}
