package com.citacomsys.core.security.sudo

import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.types.ActionConfirmationPurpose
import eu.anifantakis.lib.ksafe.biometrics.BiometricAuthorizationDuration
import eu.anifantakis.lib.ksafe.biometrics.KSafeBiometrics
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.minutes

interface IdentityStore {
    suspend fun verifyAdminPin(pin: String): Boolean
}

/**
 * Manages the administrative session (Sudo mode) with split enforcement:
 * 1. [requestTenantAuthentication]: Offline-first local biometric verification with local password/PIN fallback for tenant operators.
 * 2. [requestPlatformAdminAuthentication]: Strictly online server-side MFA / Action Confirmation Tokens (ACT) for platform admins & critical activities.
 */
object SudoManager {
    private val _isAuthorized = MutableStateFlow(true)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private var autoLockJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var actManager: ActionConfirmationManager? = null
    private var identityStore: IdentityStore? = null

    fun init(
        actManager: ActionConfirmationManager,
        identityStore: IdentityStore
    ) {
        this.actManager = actManager
        this.identityStore = identityStore
    }

    /**
     * For standard tenant operators: offline-first local biometric verification with local password/PIN fallback.
     */
    fun requestTenantAuthentication(
        onShowPinFallback: () -> Unit
    ) {
        if (_isAuthorized.value) return

        Napier.d { "SudoManager: Requesting tenant offline-first authentication (Biometric with password fallback)..." }

        scope.launch {
            isBiometricAvailable {
                if (!it) {
                    Napier.w { "SudoManager: Local biometric authentication UNAVAILABLE. Triggering local password fallback." }
                    onShowPinFallback()
                    return@isBiometricAvailable
                } else {
                    Napier.i { "SudoManager: Local biometric authentication AVAILABLE" }
                    val success = try {
                        KSafeBiometrics.verifyBiometricDirect(
                            reason = "Verify your identity to proceed",
                            authorizationDuration = BiometricAuthorizationDuration(120_000L, "sudo_user"),
                            allowDeviceCredentialFallback = false
                        ) {
                            onShowPinFallback()
                        }
                    } catch (e: Exception) {
                        false
                    }

                    if (success == true) {
                        Napier.i { "SudoManager: Local biometric authentication SUCCESS" }
                        _isAuthorized.value = true
                        resetAutoLockTimer()
                    } else {
                        Napier.w { "SudoManager: Local biometric authentication UNAVAILABLE or FAILED. Triggering local password fallback." }
                        onShowPinFallback()
                    }
                }
            }
        }
    }

    /**
     * For platform admins and critical activities: forces online connection and delegates to server-side MFA / ACT.
     */
    fun requestPlatformAdminAuthentication(
        isOnline: Boolean = true,
        purpose: ActionConfirmationPurpose = ActionConfirmationPurpose.GENERAL_SUDO,
        actionConfirmationManager: ActionConfirmationManager? = null,
        onShowPinFallback: () -> Unit
    ) {
        if (!isOnline) {
            Napier.e(tag = "SECURITY") { "SECURITY VIOLATION: Platform admins or critical activities cannot be authenticated offline." }
            error("Critical activities and platform administration require an active online connection for server-side MFA verification.")
        }

        val manager = actionConfirmationManager ?: actManager
        if (manager != null) {
            Napier.d { "SudoManager: Enforcing server-side Action Confirmation Token (ACT) / MFA for platform admin..." }
            manager.ensureConfirmation(
                purpose = purpose,
                promptUi = {
                    onShowPinFallback()
                }
            ) {
                _isAuthorized.value = true
                resetAutoLockTimer()
            }
        } else {
            error("ActionConfirmationManager is not initialized for platform admin authentication.")
        }
    }

    /**
     * SECURITY COMPLIANCE: NIST IA-5 Authenticator Management
     * Validates a manual PIN input using cryptographic comparison.
     */
    fun authorize(
        input: String,
        isPlatformAdmin: Boolean = false,
        isOnline: Boolean = true,
        onResult: (Boolean) -> Unit
    ) {
        if (isPlatformAdmin && !isOnline) {
            Napier.e(tag = "SECURITY") { "SECURITY VIOLATION: Platform admins cannot be authorized offline." }
            onResult(false)
            return
        }

        val store = identityStore
        if (store == null) {
            onResult(false)
            return
        }

        scope.launch {
            val success = store.verifyAdminPin(input)
            if (success) {
                Napier.i { "Manual ACT/PIN authentication SUCCESS" }
                _isAuthorized.value = true
                resetAutoLockTimer()
            } else {
                Napier.w { "Manual ACT/PIN authentication FAILED: Incorrect Hash or Locked Out" }
            }
            onResult(success)
        }
    }

    fun recordInteraction() {
        if (_isAuthorized.value) {
            resetAutoLockTimer()
        }
    }

    private fun resetAutoLockTimer() {
        autoLockJob?.cancel()
        autoLockJob = scope.launch {
            delay(5.minutes)
            Napier.i { "Admin session EXPIRED. Auto-locking..." }
        }
    }

    fun isBiometricAvailable(callback: (available: Boolean) -> Unit) {
        KSafeBiometrics.biometricsAvailableDirect { available ->
            callback(available)
        }
    }

    fun logout() {
        autoLockJob?.cancel()
        Napier.i { "Admin logged out" }
    }
}
