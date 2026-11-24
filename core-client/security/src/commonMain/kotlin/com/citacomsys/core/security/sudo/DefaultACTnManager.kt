package com.citacomsys.core.security.sudo

import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.security.AuthService
import com.citacomsys.types.ActionConfirmationPurpose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Sudo-mode tokens should be highly transient. 5 minutes (300s) is a secure default.
private const val ACT_LIFETIME_SECONDS = 300L

internal class DefaultACTnManager(
    private val applicationScope: CoroutineScope,
    private val authApiService: AuthService,
) : ActionConfirmationManager {

    private var expiryJob: Job? = null
    private var pendingConfirmation: PendingRequest? = null

    private val _actionToken = MutableStateFlow<String?>(null)
    override val actionToken: StateFlow<String?> = _actionToken.asStateFlow()

    // Tracks the purpose of the CURRENT active token to prevent scope-creep
    private var activeTokenPurpose: ActionConfirmationPurpose? = null

    private val _otpCode = MutableStateFlow<String?>(null)
    override val otpCode: StateFlow<String?> = _otpCode.asStateFlow()

    private val _remainingSeconds = MutableStateFlow<Long?>(null)
    override val remainingSeconds: StateFlow<Long?> = _remainingSeconds.asStateFlow()

    private data class PendingRequest(
        val purpose: ActionConfirmationPurpose,
        val onConfirmed: suspend () -> Unit
    )

    /**
     * Ensures that a valid Action Confirmation Token (ACT) exists for the specific purpose.
     */
    override fun ensureConfirmation(
        purpose: ActionConfirmationPurpose,
        promptUi: () -> Unit,
        onConfirmed: suspend () -> Unit
    ) {
        applicationScope.launch {
            // CHECK LOGIC:
            // If we have a token AND it matches the required purpose, proceed immediately.
            // If purposes don't match, we MUST re-authenticate for the new scope (Defense in Depth).
            if (_actionToken.value != null && activeTokenPurpose == purpose) {
                onConfirmed()
                return@launch
            }

            pendingConfirmation = PendingRequest(
                purpose = purpose,
                onConfirmed = onConfirmed
            )

            promptUi()
        }
    }

    override suspend fun onPasswordConfirmed(
        password: String,
        reason: String?
    ): Result<Unit> {
        // Use the pending purpose or fallback
        val targetPurpose = pendingConfirmation?.purpose ?: ActionConfirmationPurpose.GENERAL_SUDO

        val result = runCatching {
            // We pass targetPurpose.claim (e.g. "delete_resources") to the server
            authApiService.generateActionToken(password, targetPurpose.claim, reason)
        }

        result.onSuccess { token ->
            activeTokenPurpose = targetPurpose
            startTokenSession(token.token)

            pendingConfirmation?.onConfirmed?.invoke()
            pendingConfirmation = null
        }.onFailure {
            clearSession() // Clean state on failure
        }
        
        return result.map { }
    }

    override fun startTokenSession(token: String) {
        expiryJob?.cancel()
        _actionToken.value = token
        _remainingSeconds.value = ACT_LIFETIME_SECONDS

        expiryJob = applicationScope.launch {
            var remaining = ACT_LIFETIME_SECONDS
            while (remaining > 0) {
                delay(1000.milliseconds)
                remaining--
                _remainingSeconds.value = remaining
            }
            clearSession()
        }
    }

    private fun clearSession() {
        expiryJob?.cancel()
        expiryJob = null
        _actionToken.value = null
        activeTokenPurpose = null
        _remainingSeconds.value = null
        pendingConfirmation = null
    }

    override fun optCodeChanged(code: String?) { _otpCode.value = code }
}
