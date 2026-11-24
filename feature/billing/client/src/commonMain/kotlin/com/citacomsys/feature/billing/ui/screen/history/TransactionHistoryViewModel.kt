import androidx.lifecycle.viewModelScope
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.api.billing.service.BillingApiService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionHistoryViewModel(
    private val billingApiService: BillingApiService,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    private val _history = MutableStateFlow<UiResult<List<TransactionHistory>>>(UiResult.Idle)
    val history = _history.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _history.update { UiResult.Loading }
            val result = billingApiService.getPaymentHistory().asUiResult()
            _history.update { result }
        }
    }
}