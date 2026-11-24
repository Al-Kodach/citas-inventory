package com.citacomsys.feature.auth.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.security.EnrolledUser
import com.citacomsys.core.security.OfflineAuthenticator
import com.citacomsys.api.auth.AuthState
import com.citacomsys.api.auth.AuthState.OtpVerification
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.security.AuthService
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.security.sudo.OtpHandler
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.RemoteAuthResult
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class LoginState(
    val identity: String = "citacom",
    val password: String = "bewE2hNgFX#z",
    val isLoading: Boolean = false,
    val error: String? = null,
    val enrolledUsers: List<EnrolledUser> = emptyList(),
    val selectedUser: EnrolledUser? = null,
)

class AuthViewModel(
    private val authService: AuthService,
    private val tokenStore: TokenStore,
    private val sessionManager: AppSessionManager,
    kSafe: KSafe,
    offlineAuthenticator: OfflineAuthenticator,
    actionConfirmationManager: ActionConfirmationManager,
    private val telemetryClient: TelemetryLogClient
) : ViewModel() {

    private val _authState by kSafe.asMutableStateFlow<AuthState>(
        AuthState.UnAuthenticated,
        viewModelScope
    )
    val authState = _authState.asStateFlow()

    private val _loginFormState = MutableStateFlow(LoginState())
    val loginFormState = _loginFormState.asStateFlow()

    val otpHandler = OtpHandler(
        scope = viewModelScope,
        ksafe = kSafe,
        actionConfirmationManager = actionConfirmationManager
    )

    init {
        viewModelScope.launch {
            tokenStore.accessToken.collect { token ->
                if (token.isNullOrBlank() && _authState.value !is AuthState.OtpVerification) {
                    _authState.value = AuthState.UnAuthenticated
                }
            }
        }

        // Observe enrolled users for multi-user selection on this device and auto-select top account
        offlineAuthenticator.enrolledUsers
            .onEach { users ->
                _loginFormState.update { state ->
                    val topUser = users.firstOrNull()
                    state.copy(
                        enrolledUsers = users,
                        selectedUser = state.selectedUser ?: topUser,
                        identity = if (state.identity.isBlank() && state.selectedUser == null) topUser?.identity
                            ?: state.identity else state.identity
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectUser(user: EnrolledUser?) {
        _loginFormState.update {
            it.copy(
                selectedUser = user,
                password = "",
                error = null
            )
        }
    }

    private fun clearState() {
        _loginFormState.value = _loginFormState.value.copy(password = "")
    }

    fun identityChanged(identity: String) {
        // If user manually types a username, phone, email, deselect the chosen local user
        _loginFormState.update {
            it.copy(
                identity = identity,
                selectedUser = if (it.selectedUser?.identity == identity) it.selectedUser else null,
                error = null
            )
        }
    }

    fun passwordChanged(password: String) {
        _loginFormState.update { it.copy(password = password, error = null) }
    }

    fun login() {
        viewModelScope.launch {
            val password = _loginFormState.value.password
            val id = _loginFormState.value.selectedUser?.identity ?: _loginFormState.value.identity

            if (id.isBlank() || password.isBlank()) {
                if (_authState.value == AuthState.OtpVerification) {
                    _authState.value = AuthState.UnAuthenticated
                    return@launch
                }
                _loginFormState.update { it.copy(error = "Please enter both username/email/phone and password") }
                return@launch
            }

            _loginFormState.update { it.copy(isLoading = true, error = null) }

            val identity = PhoneNumber.tryCreate(id)?.normalized ?: id

            // AuthServiceImpl handles the logic for Online UnAuthenticated (with hardware enrollment)
            val result = runCatching {
                authService.login(identity, password)
            }

            _loginFormState.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                when (val authResult = result.getOrThrow()) {
                    is RemoteAuthResult.Success -> {
                        _authState.value = AuthState.Authenticated
                        clearState()
                    }

                    is RemoteAuthResult.VerificationRequired -> {
                        otpHandler.setupChallenge(authResult.challenge)
                        otpHandler.startResendTimer()
                        _authState.value = OtpVerification
                    }

                    is RemoteAuthResult.MfaRequired -> {
                        otpHandler.setupChallenge(authResult.challenge)
                        otpHandler.startResendTimer()
                        _authState.value = OtpVerification
                    }

                    RemoteAuthResult.UnregisteredDevice -> {
                        _authState.value = AuthState.SecurityLockout(isUnregistered = true)
                    }

                    RemoteAuthResult.DeviceLocked -> {
                        _authState.value = AuthState.SecurityLockout()
                    }
                }
            } else {
                val exception = result.exceptionOrNull()
                _loginFormState.update {
                    it.copy(error = exception?.message ?: "Authentication failed")
                }
            }
        }
    }

    fun otpVerify(code: String) {
        viewModelScope.launch {
            try {
                otpHandler.setLoading(true)
                val result = authService.verifyMfa(code).getOrThrow()
                authService.enrollAuthUser(
                    result.tokens,
                    result.user,
                    _loginFormState.value.password
                )
                otpHandler.onVerified()
                _authState.value = AuthState.Authenticated
                clearState()
            } catch (e: Exception) {
                otpHandler.setError(e.asUiError().message)
            }

        }
    }

    fun navigateToLogin() {
        _authState.value = AuthState.UnAuthenticated
    }

    fun navigateToResetPassword() {
        _authState.value = AuthState.ResetPassword
    }

    fun logout() {
        Napier.d { "Logout requested" }
        viewModelScope.launch {
            val user = sessionManager.getUser()

            if (user != null) {
                runCatching {
                    telemetryClient.trackIdentityEvent(
                        userId = user.id,
                        action = "LOGOUT",
                        outcome = "SUCCESS"
                    )
                }
            }
            authService.logout()
        }
    }
}
