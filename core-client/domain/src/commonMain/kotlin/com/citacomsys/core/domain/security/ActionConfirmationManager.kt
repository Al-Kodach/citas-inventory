package com.citacomsys.core.domain.security

import com.citacomsys.types.ActionConfirmationPurpose
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages Action Tokens (ACT) and temporary OTP codes for sensitive operations.
 * Acts as a centralized state for client-side HTTP plugins to attach security headers.
 */
interface ActionConfirmationManager {

    val remainingSeconds: StateFlow<Long?>
    val actionToken: StateFlow<String?>
    val otpCode: StateFlow<String?>

    /**
     * The primary method called by ViewModels. It ensures a valid ACT exists,
     * prompting the user for a password if necessary.
     */
    fun ensureConfirmation(
        purpose: ActionConfirmationPurpose = ActionConfirmationPurpose.GENERAL_SUDO,
        promptUi: () -> Unit,
        onConfirmed: suspend () -> Unit
    )

    /**
     * Called by the PasswordPromptManager after the user enters their password.
     */
    suspend fun onPasswordConfirmed(
        password: String,
        reason: String? = null
    ): Result<Unit>

    /**
     * Manually starts an action session. Used for flows like MFA setup
     * where the token is obtained via OTP instead of a password prompt.
     */
    fun startTokenSession(token: String)

    /**
     * Temporarily sets an OTP code to be attached to outgoing requests via X-OTP-Code header.
     */
    fun optCodeChanged(code: String?)
}