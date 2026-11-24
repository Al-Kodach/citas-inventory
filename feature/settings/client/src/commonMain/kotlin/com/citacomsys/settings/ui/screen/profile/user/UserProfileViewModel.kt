package com.citacomsys.settings.ui.screen.profile.user

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.security.sudo.MfaAuthManager
import com.citacomsys.feature.settings.api.UserSecurityApi
import com.citacomsys.types.ProductType
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val api: UserSecurityApi,
    notificationService: InAppNotificationService,
    private val sessionManager: AppSessionManager,
    actManager: ActionConfirmationManager,
    kSafe: KSafe,
) : BaseViewModel(notificationService) {

    val productType = sessionManager.activeProductType

    val authManager = MfaAuthManager(
        viewModelScope,
        kSafe,
        actManager
    )

    val appUser = sessionManager.appUser.asUiResult()

    private val _actionState = MutableStateFlow<UiResult<Any>>(UiResult.Idle)
    val actionState = _actionState.asStateFlow()

    private val _showOtpPrompt = MutableStateFlow(false)
    val showOtpPrompt = _showOtpPrompt.asStateFlow()

    private val _showPasswordPrompt = MutableStateFlow(false)
    val showPasswordPrompt = _showPasswordPrompt.asStateFlow()

    private var isDisablingMfa = false

    private inline fun <reified T> executeAction(
        crossinline action: suspend () -> Result<T>,
        crossinline onSuccess: suspend (T) -> Unit
    ) {
        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            action().onSuccess { data ->
                _actionState.value = UiResult.Idle
                onSuccess(data)
            }.onFailure { error ->
                _actionState.value = error.asUiError()
            }
        }
    }

    fun initiateMfaSetup() {
        isDisablingMfa = false
        executeAction(
            action = { api.requestMfaEnable() },
            onSuccess = { challenge ->
                authManager.setupChallenge(challenge)
                authManager.startResendTimer()
                _showOtpPrompt.value = true
            }
        )
    }

    fun initiateMfaDisable() {
        isDisablingMfa = true
        executeAction(
            action = { api.requestMfaDisableAct() },
            onSuccess = { challenge ->
                authManager.setupChallenge(challenge)
                authManager.startResendTimer()
                _showOtpPrompt.value = true
            }
        )
    }

    fun isDisablingMfa(): Boolean = isDisablingMfa

    fun resendMfaCode() {
        if (isDisablingMfa) initiateMfaDisable() else initiateMfaSetup()
    }

    fun verifyMfaAction(otp: String) {
        if (isDisablingMfa) {
            confirmMfaDisable(otp)
        } else {
            verifyMfaSetup(otp)
        }
    }

    private fun verifyMfaSetup(otp: String) {
        viewModelScope.launch {
            authManager.setLoading(true)
            val confirmResult = api.confirmMfaEnable(otp)
            if (confirmResult.isSuccess) {
                authManager.onVerified()
                val user = sessionManager.getUser()!!.copy(mfaEnabled = true)
                sessionManager.setAppUser(user)
                _showOtpPrompt.value = false
                showSuccess("MFA enabled successfully")
            } else {
                authManager.setError(confirmResult.exceptionOrNull()?.message ?: "Invalid OTP code")
            }
        }
    }

    private fun confirmMfaDisable(otp: String) {
        viewModelScope.launch {
            authManager.setLoading(true)
            val confirmResult = api.disableMfa(otp)
            if (confirmResult.isSuccess) {
                authManager.onVerified()
                val user = sessionManager.getUser()!!.copy(mfaEnabled = false)
                sessionManager.setAppUser(user)
                _showOtpPrompt.value = false
                isDisablingMfa = false
                showSuccess("MFA has been disabled")
            } else {
                authManager.setError(confirmResult.exceptionOrNull()?.message ?: "Invalid OTP code")
            }
        }
    }

    fun dismissPrompts() {
        _actionState.value = UiResult.Idle
        _showOtpPrompt.value = false
        _showPasswordPrompt.value = false
        isDisablingMfa = false
        authManager.clear()
    }

    fun handleActionConfirmation(password: String) {
        _showPasswordPrompt.value = false
    }
}
