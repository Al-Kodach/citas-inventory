package com.citacomsys.inventory.admin.auth

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.auth.AdminAuthService
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.BindDeviceResponse
import com.citacomsys.types.RemoteAuthResult
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.security.sudo.OtpHandler
import com.citacomsys.core.ui.BaseViewModel
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val identity: String = "0598479398",
    val password: String = "password123",
    val performOtp: Boolean = false,
    val mfaChallenge: AuthChallengeContext? = null,
    val result: UiResult<Unit> = UiResult.Idle
) {
    val canLogin = identity.isNotBlank() && password.isNotBlank()
    val isLoading = result is UiResult.Loading
}

class AdminAuthViewModel(
    private val authApiService: AdminAuthService,
    private val tokenStore: TokenStore,
    private val sessionManager: AppSessionManager,
    private val prefs: KSafe,
    notificationService: InAppNotificationService,
    actionConfirmationManager: ActionConfirmationManager
) : BaseViewModel(notificationService) {

    val user = sessionManager.appUser

    val otpHandler = OtpHandler(
        viewModelScope,
        prefs,
        actionConfirmationManager,
    )

    private val loginState = MutableStateFlow(LoginState())
    val state = loginState.asStateFlow()

    private val _isLocked by prefs.asMutableStateFlow(false, viewModelScope, "device_locked_state")
    private val _bindFlowState by prefs.asMutableStateFlow<BindDeviceResponse?>(null, viewModelScope, "bind_flow_state")

    fun identityChanged(identity: String) {
        loginState.value = loginState.value.copy(
            identity = identity,
            result = UiResult.Idle
        )
    }

    fun passwordChanged(password: String) {
        loginState.value = loginState.value.copy(
            password = password,
            result = UiResult.Idle
        )
    }

    fun setResult(result: UiResult<Unit>) {
        loginState.value = loginState.value.copy(result = result)
    }

    fun login() {
        viewModelScope.launch {
            setResult(UiResult.Loading)
            val (identity, password) = loginState.value

            try {
                when (val result = authApiService.login(identity, password)) {
                    is RemoteAuthResult.MfaRequired -> {
                        val challenge = result.challenge
                        loginState.value = loginState.value.copy(
                            performOtp = true,
                            mfaChallenge = challenge,
                            result = UiResult.Idle
                        )
                        otpHandler.setupChallenge(challenge)
                        otpHandler.startResendTimer()
                    }
                    is RemoteAuthResult.VerificationRequired -> {
                        val challenge = result.challenge
                        loginState.value = loginState.value.copy(
                            performOtp = true,
                            mfaChallenge = challenge,
                            result = UiResult.Idle
                        )
                        otpHandler.setupChallenge(result.challenge)
                        otpHandler.startResendTimer()
                    }

                    is RemoteAuthResult.Success -> {
                        tokenStore.save(result.session.tokens)
                        sessionManager.setAppUser(result.session.user)
                    }

                    is RemoteAuthResult.DeviceLocked -> {
                        setResult(UiResult.Error("Security Violation: This device has been administratively locked."))
                        _isLocked.value = true
                        tokenStore.clearAll()
                    }
                    is RemoteAuthResult.UnregisteredDevice -> {
                        setResult(UiResult.Error("Unrecognized Hardware: This device must be bound to the platform before use."))
                        tokenStore.clearAll()
                        _bindFlowState.value = null
                    }
                }
            } catch (e: Exception) {
                setResult(e.asUiError())
            }
        }
    }

    fun verifyMfa(otpCode: String) {
        viewModelScope.launch {
            otpHandler.setLoading(true)
            val currentState = loginState.value
            val challenge = currentState.mfaChallenge ?: return@launch

            try {
                authApiService.verifyMfa(challenge.userId, otpCode)
                    .onSuccess { session ->
                        tokenStore.save(session.tokens)
                        sessionManager.setAppUser(session.user)
                        otpHandler.onVerified()
                        loginState.value = LoginState()
                    }
                    .onFailure { e ->
                        otpHandler.setError(e.message ?: "Verification failed")
                        setResult(e.asUiError())
                    }
            } catch (e: Exception) {
                otpHandler.setError(e.message ?: "Verification failed")
                setResult(e.asUiError())
            }
        }
    }

    fun cancelOtp() {
        otpHandler.clear()
        loginState.value = loginState.value.copy(performOtp = false)
    }

    fun logout() {
        viewModelScope.launch {
            tokenStore.clearAll()
            _isLocked.value = false
            _bindFlowState.value = null
            sessionManager.setAppUser(null)
            authApiService.logout()
        }
    }
}
