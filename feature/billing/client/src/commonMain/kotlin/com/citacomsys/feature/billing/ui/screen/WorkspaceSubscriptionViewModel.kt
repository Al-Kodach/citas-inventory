package com.citacomsys.feature.billing.ui.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.api.billing.service.BillingApiService
import com.citacomsys.api.workspace.model.SubscriptionSummary
import com.citacomsys.api.workspace.service.WorkspaceApiService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.shared.util.mapStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class TenantSubscriptionUiState(
    val packageName: String,
    val expiryDate: String,
    val status: String
)


data class SubscriptionHubState(
    val planInfo: TenantSubscriptionUiState? = null,
    val recentTransactions: UiResult<List<TransactionHistory>> = UiResult.Idle,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WorkspaceSubscriptionViewModel(
    notificationService: InAppNotificationService,
    private val tenantApiService: WorkspaceApiService,
    private val billingApiService: BillingApiService,
    private val service: PlatformClientService
) : BaseViewModel(notificationService) {

    private val _state = MutableStateFlow(SubscriptionHubState())
    val state = _state.asStateFlow()

    val configState = service.activeWorkspaceConfig.asUiResult()
        .mapStateFlow(viewModelScope, UiResult.Loading) { it }

    init {
        refresh()
    }

    fun refresh() {
        loadSubscriptionData()
        loadHistory()
    }

    fun refreshConfig() {
        viewModelScope.launch {
            showInfo("Loading...")
            service.refreshWorkspaceConfig()
                .onSuccess { showSuccess("Success") }
                .onFailure { showSuccess(it.message ?: "Fail to refresh config") }
        }
    }

    private fun loadSubscriptionData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            runCatching {
                // Fetch the comprehensive summary from the Tenant API
                tenantApiService.getSubscriptionSummary()
            }.onSuccess { summary ->
                _state.update {
                    it.copy(
                        planInfo = summary.toPlanUiState(),
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(recentTransactions = UiResult.Loading) }
            val result = billingApiService.getPaymentHistory().asUiResult()
            _state.update { it.copy(recentTransactions = result) }
        }
    }

    private fun SubscriptionSummary.toPlanUiState() = TenantSubscriptionUiState(
        packageName = this.packageName,
        expiryDate = this.expiresAt?.let { instant ->
            val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${
                dt.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            } ${dt.day}, ${dt.year}"
        } ?: "Never",
        status = this.status.name
    )
}
