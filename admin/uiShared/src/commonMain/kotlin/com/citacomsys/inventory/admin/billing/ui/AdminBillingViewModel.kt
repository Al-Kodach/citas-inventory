package com.citacomsys.inventory.admin.billing.ui

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.billing.AdminBillingApiService
import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.types.User
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AdminBillingViewModel(
    private val billingService: AdminBillingApiService,
    prefs: KSafe,
    notification: InAppNotificationService,
) : BaseViewModel(notification) {

    private val activeUser by prefs.asMutableStateFlow<User?>(null, viewModelScope)
    val user = activeUser.asStateFlow()

    private val _activeInvoiceState = MutableStateFlow<UiResult<List<BillInvoice>>>(UiResult.Idle)
    val activeInvoiceState = _activeInvoiceState.asStateFlow()


    init {
        viewModelScope.launch {
            EventBus.subscribe<EntityChangeEvent>().collect { event ->
                if (event.entityType == "BILLING") {
                    val orgId = activeUser.value?.organizationId
                    if (orgId.isNullOrBlank() || event.organizationId == orgId) {
                        loadActiveBills()
                    }
                }
            }
        }

        viewModelScope.launch {
            EventBus.allEvents.collect {
                showInfo(it.importance.name + " Event received")
            }
        }
    }

    fun loadActiveBills() {
        if (_activeInvoiceState.value.isLoading()) return

        viewModelScope.launch {
            _activeInvoiceState.value = UiResult.Loading
            _activeInvoiceState.value = runCatching {
                billingService.fetch()
            }.asUiResult()
        }
    }
}
