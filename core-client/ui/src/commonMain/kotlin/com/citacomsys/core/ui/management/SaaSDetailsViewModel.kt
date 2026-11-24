package com.citacomsys.core.ui.management

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.security.sudo.ActorSudoAuthHandler
import com.citacomsys.types.ActionConfirmationPurpose
import com.citacomsys.types.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A generic base ViewModel for SaaS detail screens (Staff, Product, Customer).
 */
abstract class SaaSDetailsViewModel<T>(
    notificationService: InAppNotificationService,
    val sudoAuthHandler: ActorSudoAuthHandler
) : BaseViewModel(notificationService) {

    private val _selectedItemState = MutableStateFlow<UiResult<T>>(UiResult.Idle)
    val selectedItemState: StateFlow<UiResult<T>> = _selectedItemState.asStateFlow()

    /**
     * Implementing classes must define how to fetch the specific item details.
     */
    abstract suspend fun getDetails(itemId: String): T

    /**
     * Loads the details for the given ID and updates the [selectedItemState].
     */
    fun loadDetails(id: String) {
        viewModelScope.launch {
            _selectedItemState.value = UiResult.Loading
            _selectedItemState.value = runCatching { getDetails(id) }.asUiResult()
        }
    }

    /**
     * Helper to perform a sensitive action with sudo authentication.
     */
    fun <R> authenticateAndExecute(
        title: String,
        purpose: ActionConfirmationPurpose,
        action: suspend () -> R,
        onResult: suspend (R) -> Unit
    ) {
        sudoAuthHandler.authenticateAndExecute(
            title = title,
            purpose = purpose,
            onResult = onResult,
            action = action
        )
    }

    fun clearState() {
        _selectedItemState.value = UiResult.Idle
        sudoAuthHandler.dismissPrompts()
    }
}
