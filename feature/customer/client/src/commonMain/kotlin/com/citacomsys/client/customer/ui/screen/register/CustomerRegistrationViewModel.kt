package com.citacomsys.client.customer.ui.screen.register

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.AppOperatingMode
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.module.api.customer.Customer
import com.citacomsys.module.api.customer.CustomerRegistrationRequest
import com.citacomsys.module.api.customer.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

data class StateInfo(
    val info: String? = null,
    val enableSaveForm: Boolean = false,
)

internal class CustomerRegistrationViewModel(
    private val customerRepository: CustomerRepository,
    private val sessionManager: AppSessionManager,
    notificationService: InAppNotificationService,
    manifest: AppManifest,
) : BaseViewModel(notificationService) {

    private val _saveState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val saveState = _saveState.asStateFlow()

    private val _state = MutableStateFlow(StateInfo())
    val state = _state.asStateFlow()

    private val _customerState = MutableStateFlow<UiResult<Customer>>(UiResult.Idle)
    val customerState = _customerState.asStateFlow()

    val fullName = MutableStateFlow("")
    val phone = MutableStateFlow("")
    val email = MutableStateFlow("")
    val creditLimitInput = MutableStateFlow("")
    val isAlertsConsented = MutableStateFlow(true)
    private var isEditMode = false

    init {
        viewModelScope.launch {
            combine(
                sessionManager.activeWorkspaceIdFlow,
                customerState,
                manifest.operatingMode
            ) { wsId, customer, opp ->
                val canCreateCustomer = wsId != null
                val canEdit = customer.getOrNull() != null

                val info = if (canCreateCustomer) {
                    val wsInfo = sessionManager.workStationInfo.value
                    "Registering customer for ${wsInfo.activeWorkspaceName} (${wsInfo.activeBranchName})"
                } else "Launch a business workspace to create customer"

                _state.value = StateInfo(
                    info = info,
                    enableSaveForm = canCreateCustomer || canEdit
                )
            }.launchIn(this)
        }
    }

    fun load(customerId: String?, workspaceId: String?) {
        if (customerId == null) return
        if (workspaceId == null) return

        isEditMode = true

        viewModelScope.launch {
            _customerState.value = UiResult.Loading
            try {
                val customer = customerRepository.findById(customerId, workspaceId).getOrNull()
                if (customer != null) {
                    fullName.value = customer.fullName
                    phone.value = customer.phone ?: ""
                    email.value = customer.email ?: ""
                    val limitMinor = customer.creditLimit?.amountMinor
                    creditLimitInput.value =
                        if (limitMinor != null) (limitMinor / 100.0).toString() else ""
                    isAlertsConsented.value = customer.isAlertsConsented
                    _customerState.value = UiResult.Success(customer)
                } else {
                    _customerState.value = UiResult.Error("Customer not found")
                }
            } catch (e: Exception) {
                _customerState.value = UiResult.Error(e.asUiError().message)
            }
        }
    }

    fun save() {
        val workspaceId =
            if (isEditMode) customerState.value.getOrNull()?.workspaceId else sessionManager.activeWorkspaceId
        if (workspaceId == null) {
            _saveState.value = UiResult.Error("Business not configured")
            return
        }

        viewModelScope.launch {
            _saveState.value = UiResult.Loading
            try {
                val limitMinor =
                    creditLimitInput.value.toDoubleOrNull()?.let { (it * 100).toLong() }
                val request = CustomerRegistrationRequest(
                    fullName = fullName.value,
                    phone = phone.value.ifBlank { null },
                    email = email.value.ifBlank { null },
                    creditLimitMinor = limitMinor,
                    isConsented = isAlertsConsented.value,
                    branchId = sessionManager.resolveWriteBranchId(),
                    workspaceId = workspaceId,
                    actorId = sessionManager.currentUserId!!,
                    actorName = sessionManager.appUser.value!!.name
                )

                _saveState.value = runCatching {
                    customerRepository.getOrCreateCustomer(request, workspaceId)
                    Unit
                }.asUiResult()
            } catch (e: Exception) {
                _saveState.value = e.asUiError()
                showError(e.asUiError().message)
            }
        }
    }
}
