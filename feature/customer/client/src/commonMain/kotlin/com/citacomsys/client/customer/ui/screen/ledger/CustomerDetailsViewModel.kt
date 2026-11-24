package com.citacomsys.client.customer.ui.screen.ledger

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.repository.GlobalSearchRepository
import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.module.api.customer.Customer
import com.citacomsys.module.api.customer.CustomerLedgerRequest
import com.citacomsys.module.api.customer.CustomerRepository
import com.citacomsys.module.api.customer.CustomerTransactionType
import com.citacomsys.types.EntityType
import com.citacomsys.types.UserCapability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CustomerDetailsViewModel(
    private val customerId: String,
    private val workspaceId: String,
    private val customerRepository: CustomerRepository,
    private val globalSearchRepository: GlobalSearchRepository,
    private val sessionManager: AppSessionManager,
    notificationService: InAppNotificationService,
    capsManager: UserCapabilityManager
) : BaseViewModel(notificationService) {

    private val _customerState = MutableStateFlow<UiResult<Customer>>(UiResult.Idle)
    val customerState = _customerState.asStateFlow()

    private val _saleSearchResults =
        MutableStateFlow<UiResult<List<SearchResult>>>(UiResult.Success(emptyList()))
    val saleSearchResults: StateFlow<UiResult<List<SearchResult>>> =
        _saleSearchResults.asStateFlow()
    val canManagerCustomer = capsManager.has(UserCapability.MANAGE_CUSTOMERS)

    init {
        loadDetails()
    }

    fun loadDetails() {
        if (_customerState.value.isLoading) return
        viewModelScope.launch {
            _customerState.value = UiResult.Loading
            _customerState.value = customerRepository.findById(customerId, workspaceId).asUiResult()
        }
    }

    fun searchSales(query: String) {
        val clean = query.trim()
        if (clean.length < 2) {
            _saleSearchResults.value = UiResult.Success(emptyList())
            return
        }
        viewModelScope.launch {
            _saleSearchResults.value = UiResult.Loading
            runCatching {
                globalSearchRepository.search(clean).first().filter { it.type == EntityType.SALE }
            }.onSuccess { list ->
                _saleSearchResults.value = UiResult.Success(list)
            }.onFailure { err ->
                _saleSearchResults.value = UiResult.Error(err.message ?: "Failed to search sales")
            }
        }
    }

    fun recordTransaction(
        amountMinor: Long,
        currencyCode: String,
        type: CustomerTransactionType,
        method: String?,
        reference: String?,
        note: String?
    ) {
        viewModelScope.launch {
            runCatching {
                val request = CustomerLedgerRequest(
                    workspaceId = workspaceId,
                    amountMinor = amountMinor,
                    currency = currencyCode,
                    type = type,
                    method = method,
                    reference = reference,
                    note = note,
                    actorId = sessionManager.currentUserId!!,
                    actorName = sessionManager.appUser.value!!.name,
                    branchId = sessionManager.resolveWriteBranchId()
                )
                customerRepository.recordTransaction(customerId, request)
            }.onSuccess {
                showSuccess("Transaction recorded successfully")
                loadDetails()
            }.onFailure {
                showError("Failed to record transaction: ${it.message}")
            }
        }
    }
}
