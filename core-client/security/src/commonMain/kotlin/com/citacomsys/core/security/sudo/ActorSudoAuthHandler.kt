package com.citacomsys.core.security.sudo

import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.types.ActionConfirmationPurpose
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.biometrics.BiometricAuthorizationDuration
import eu.anifantakis.lib.ksafe.biometrics.KSafeBiometrics
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActorSudoState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showOtpPrompt: Boolean = false,
    val showPasswordPrompt: Boolean = false,
    val showIcon: Boolean = true,
    val showResend: Boolean = true,
    val otpLength: Int = 6
)

class ActorSudoAuthHandler(
    private val actionConfirmationManager: ActionConfirmationManager,
    sessionManager: AppSessionManager,
    private val scope: CoroutineScope,
    prefs: KSafe
) {
    val activeUser = sessionManager.appUser
    private val _showPasswordPrompt = MutableStateFlow(false)
    private val _showOtpPrompt = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val pwdError = MutableStateFlow<String?>(null)


    val otpHandler: OtpHandler = OtpHandler(
        scope = scope,
        actionConfirmationManager = actionConfirmationManager,
        ksafe = prefs
    )

    val otpLoading = otpHandler.isLoading
    val otpError = otpHandler.error

    val errorMessage = combine(pwdError, otpError) { pwdErr, otpErr -> pwdErr ?: otpErr }


    val uiState = combine(
        _showOtpPrompt,
        _showPasswordPrompt,
        errorMessage,
        bindFlow(_isLoading, otpLoading) {
            it.firstOrNull { it } ?: false
        },
    ) { otpP, pwdP, err, loading ->
        ActorSudoState(
            showOtpPrompt = otpP,
            showPasswordPrompt = pwdP,
            isLoading = loading,
            errorMessage = err
        )
    }.stateIn(scope, SharingStarted.Lazily, ActorSudoState())

    fun passwordConfirmed(password: String) {
        scope.launch {
            _isLoading.value = true
            val result = actionConfirmationManager.onPasswordConfirmed(password)
            if (result.isFailure) {
                val errorMessage = result.exceptionOrNull()?.message
                pwdError.value = errorMessage
            } else {
                dismissPrompts()
            }
            _isLoading.value = false
        }
    }

    fun otpConfirm(code: String) {
        // ACTManager + Injector handles this
    }

    fun <T> authenticateAndExecute(
        title: String,
        purpose: ActionConfirmationPurpose,
        action: suspend () -> T,
        onResult: suspend (T) -> Unit = {},
    ) {
        actionConfirmationManager.ensureConfirmation(
            purpose = purpose,
            promptUi = {
                Napier.d { "Show prompt" }
                KSafeBiometrics.verifyBiometricDirect(
                    reason = title,
                    authorizationDuration = BiometricAuthorizationDuration(
                        120_000L, "user_${activeUser.value?.id}"
                    ),
                    allowDeviceCredentialFallback = false // Only biometric (No device pin/password)
                ) {
                    _showPasswordPrompt.value = true
                }
            }
        ) {
            onResult(action())
        }
    }

    fun dismissPrompts() {
        _showPasswordPrompt.value = false
        _showOtpPrompt.value = false
        _isLoading.value = false
        pwdError.value = null
        otpHandler.clear()
        otpHandler.cancel()
    }
}

inline fun <reified T : Any, reified R : Any> bindFlow(
    flow1: Flow<T>,
    flow2: Flow<T>,
    crossinline transform: (Array<T>) -> R
): Flow<R> {
    return combine(flow1, flow2) {
        transform(it)
    }
}