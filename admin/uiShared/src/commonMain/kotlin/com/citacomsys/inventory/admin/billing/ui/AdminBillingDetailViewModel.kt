package com.citacomsys.inventory.admin.billing.ui

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.billing.AdminBillingApiService
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.security.sudo.ActorSudoAuthHandler
import com.citacomsys.inventory.admin.common.ui.AdminBaseDetailsViewModel
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.ActionConfirmationPurpose
import com.citacomsys.shared.event.EntityChangeEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminBillingDetailViewModel(
    notificationService: InAppNotificationService,
    private val billingService: AdminBillingApiService,
    sudoAuthHandler: ActorSudoAuthHandler,
) : AdminBaseDetailsViewModel<BillingRecord>(
    notificationService, sudoAuthHandler
) {

    private val _recordState = MutableStateFlow<UiResult<BillingRecord>?>(null)
    val recordState = _recordState.asStateFlow()


    init {
        viewModelScope.launch {
            EventBus.subscribe<EntityChangeEvent>().collect { event ->
                if (event.entityId == recordState.value?.getOrNull()?.id) {
                    loadDetails(event.entityId)
                }
            }
        }
    }

    override suspend fun getDetails(itemId: String): BillingRecord {
        return billingService.getRecord(itemId)
    }

    fun clearRecord() {
        _recordState.value = null
        sudoAuthHandler.dismissPrompts()
    }

    fun verify(invoice: BillingRecord) {
        viewModelScope.launch {
            authenticateAndExecute(
                purpose = ActionConfirmationPurpose.VERIFY_PAYMENT,
                title = "Authenticate to verify payment",
                action = {
                    runCatching {
                        _recordState.value = UiResult.Loading
                        billingService.verified(invoice.referenceCode)
                    }
                }
            ) { result ->
                if (result.isSuccess) {
                    if (_recordState.value is UiResult.Success
                        && (_recordState.value as UiResult.Success).data.id == invoice.id
                    ) {
                        loadDetails(invoice.id)
                    }
                }
            }
        }
    }

    fun reject(invoiceId: String, reason: String) {
        viewModelScope.launch {
            authenticateAndExecute(
                purpose = ActionConfirmationPurpose.REJECT_PAYMENT,
                title = "Authenticate to reject payment",
                action = {
                    runCatching {
                        _recordState.value = UiResult.Loading
                        billingService.reject(invoiceId, reason)
                    }
                }
            ) { result ->
                if (result.isSuccess) {
                    if (_recordState.value is UiResult.Success &&
                        (_recordState.value as UiResult.Success).data.id == invoiceId
                    ) {
                        loadDetails(invoiceId)
                    }
                }
            }
        }
    }

    fun passwordConfirmation(password: String) {
        sudoAuthHandler.passwordConfirmed(password)
    }
}