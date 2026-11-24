package com.citacomsys.feature.auth.ui.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.security.AuthService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.security.sudo.OtpHandler
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.shared.validation.Validators
import com.citacomsys.shared.validation.addNotNull
import com.citacomsys.shared.validation.validate
import com.citacomsys.types.AuthChallengeContext
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val otp: String = "",

    val identifier: String = "",
    val challenge: AuthChallengeContext? = null,
    val phase: ResetPhase = ResetPhase.IDENTIFIER,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
) {
    @Serializable
    enum class ResetPhase {
        IDENTIFIER,
        OTP_AND_PASSWORD
    }
}

class ResetPasswordViewModel(
    private val authApiService: AuthService,
    notificationService: InAppNotificationService,
    private val actionConfirmationManager: ActionConfirmationManager,
    kSafe: KSafe
) : BaseViewModel(notificationService) {

    val otpHandler: OtpHandler = OtpHandler(
        viewModelScope,
        kSafe,
        actionConfirmationManager
    )

    private val _state by kSafe.asMutableStateFlow(
        ResetPasswordUiState(),
        viewModelScope
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun identifierChange(value: String) {
        _state.update { it.copy(identifier = value) }
    }

    fun otpChange(value: String) {
        _state.update { it.copy(otp = value) }
        actionConfirmationManager.optCodeChanged(value)
    }

    fun newPasswordChange(value: String) {
        _state.update { it.copy(newPassword = value) }
    }

    fun confirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value) }
    }

    private fun validatePasswordReset() {
        val errors = mutableListOf<String>()
        val (newPassword, confirmPassword) = state.value

        errors.addNotNull(
            validate(
                newPassword,
                Validators.required(),
                Validators.minLength(8)
            ).errorMessage?.let { "Password $it" },
            validate(
                confirmPassword,
                Validators.matches(newPassword, "Passwords do not match")
            ).errorMessage
        )

        if (errors.isNotEmpty()) {
            _state.update { it.copy(error = errors.first()) }
            return
        }
    }

    /**
     * Phase 1: Request an OTP for the given identifier (Email/Phone)
     */
    fun requestReset() {
        viewModelScope.launch {
            val input = _state.value.identifier.trim()
            val isEmail = input.contains("@") && input.contains(".")
            val isPhone = input.startsWith("+") || (input.length >= 7 && input.all { it.isDigit() || it == '+' || it == ' ' || it == '-' })

            if (input.isBlank() || (!isEmail && !isPhone)) {
                _state.update { it.copy(error = "Please enter a valid phone number or email address") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }
            authApiService.requestPasswordReset(input).fold(
                onSuccess = { challengeContext ->
                    otpHandler.setupChallenge(challengeContext)
                    otpHandler.startResendTimer()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            challenge = challengeContext,
                            phase = ResetPasswordUiState.ResetPhase.OTP_AND_PASSWORD
                        )
                    }
                    showSuccess("Reset code sent successfully")
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to request resetWorkspaceSession"
                    _state.update { it.copy(isLoading = false, error = msg) }
                    showError(msg)
                }
            )
        }
    }

    /**
     * Phase 2: Submit the OTP and new password
     */
    fun submitReset(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val challengeContext = _state.value.challenge
            val otpCode = actionConfirmationManager.otpCode.value.takeUnless { it.isNullOrBlank() }
                ?: _state.value.otp

            if (challengeContext == null) {
                showError("Invalid resetWorkspaceSession session. Please request a new code.")
                return@launch
            }

            if (otpCode.isBlank()) {
                _state.update { it.copy(error = "Please enter the OTP verification code") }
                return@launch
            }

            validatePasswordReset()
            if (_state.value.error != null) return@launch

            _state.update { it.copy(isLoading = true, error = null) }
            otpHandler.setLoading(true)

            authApiService.submitPasswordReset(
                challenge = challengeContext,
                otp = otpCode,
                newPassword = _state.value.newPassword
            ).fold(
                onSuccess = {
                    otpHandler.onVerified()
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                    showSuccess("Password updated successfully")
                    onSuccess()
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to update password"
                    otpHandler.setError(msg)
                    _state.update { it.copy(isLoading = false, error = msg) }
                    showError(msg)
                }
            )
        }
    }

    fun clearState() {
        otpHandler.clear()
        _state.value = ResetPasswordUiState()
    }
}
