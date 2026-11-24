package com.citacomsys.core.security.sudo

import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.ChallengeType
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.invoke
import eu.anifantakis.lib.ksafe.getStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@Serializable
data class OtpVerificationState(
    val title: String = "Verify Ownership",
    val description: String = "",
    val identifierMasked: String = "",
    val verifyButtonText: String = "Verify",
    val sessionId: String? = null,
    val resendCountdown: Int = 0,
    val lastRequestTimestamp: Long = 0,
    val isVerified: Boolean = false,
    val allowEmergencyRecovery: Boolean = false,
    val otpLength: Int = 6,
    val challengeType: ChallengeType = ChallengeType.OTP,
    val metadata: Map<String, String> = emptyMap()
)

class MfaAuthManager(
    private val scope: CoroutineScope,
    ksafe: KSafe,
    private val actionConfirmationManager: ActionConfirmationManager,
    private val resendIntervalSeconds: Int = 300
) {
    private val stateKey = "mfa_auth_state"

    private var state by ksafe(OtpVerificationState(), stateKey)

    val stateFlow = ksafe.getStateFlow(stateKey, state, scope)

    private val _resendCountdown = MutableStateFlow(0)
    val resendCountdown = _resendCountdown.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var timerJob: Job? = null

    init {
        resumeTimerIfNeeded()
    }

    @OptIn(ExperimentalTime::class)
    private fun resumeTimerIfNeeded() {
        val currentState = state
        if (currentState.lastRequestTimestamp > 0 && currentState.resendCountdown > 0) {
            val now = Clock.System.now().toEpochMilliseconds()
            val secondsElapsed = ((now - currentState.lastRequestTimestamp) / 1000).toInt()
            val remaining = (currentState.resendCountdown - secondsElapsed).coerceAtLeast(0)

            _resendCountdown.value = remaining
            if (remaining > 0) {
                startTimerLoop()
            } else {
                state = currentState.copy(resendCountdown = 0, lastRequestTimestamp = 0)
            }
        }
    }

    fun setupChallenge(
        challenge: AuthChallengeContext,
        overrideTitle: String? = null,
        overrideVerifyButtonText: String? = null
    ) {
        val identifier = challenge.accountMasked ?: ""

        val (purposeTitle, purposeDesc, purposeButtonText) = when (challenge.challengeType) {
            ChallengeType.PASSWORD -> Triple(
                "Password Verification",
                "Enter your account password to authorize '${challenge.action}'",
                "Confirm Password"
            )
            ChallengeType.BIOMETRIC -> Triple(
                "Biometric Verification",
                "Authenticate using biometrics to proceed",
                "Authenticate"
            )
            ChallengeType.ACTION_CONFIRMATION_TOKEN -> Triple(
                "Security Confirmation",
                "Confirm sensitive action: ${challenge.action}",
                "Authorize Action"
            )
            else -> Triple(
                challenge.title.ifBlank { "Verify Identity" },
                challenge.description.ifBlank { "Enter the ${challenge.codeLength}-digit verification code sent to" },
                "Verify & Continue"
            )
        }

        val title = overrideTitle ?: purposeTitle
        val verifyButtonText = overrideVerifyButtonText ?: purposeButtonText

        setup(
            title = title,
            description = purposeDesc,
            identifierMasked = identifier,
            verifyButtonText = verifyButtonText,
            otpLength = challenge.codeLength,
            challengeType = challenge.challengeType,
            metadata = mapOf(
                "challengeId" to challenge.challengeId,
                "action" to challenge.action,
                "otpPurpose" to challenge.otpPurpose,
                "userId" to challenge.userId,
                "organizationId" to challenge.organizationId
            ) + challenge.metadata
        )
    }

    fun setup(
        title: String,
        description: String,
        identifierMasked: String,
        sessionId: String? = null,
        allowEmergency: Boolean = false,
        metadata: Map<String, String> = emptyMap(),
        verifyButtonText: String = "Verify",
        otpLength: Int = 6,
        challengeType: ChallengeType = ChallengeType.OTP
    ) {
        state = state.copy(
            title = title,
            description = description,
            verifyButtonText = verifyButtonText,
            identifierMasked = identifierMasked,
            sessionId = sessionId,
            allowEmergencyRecovery = allowEmergency,
            metadata = metadata,
            otpLength = otpLength,
            challengeType = challengeType,
            isVerified = false
        )
        if (timerJob == null || timerJob?.isActive == false) {
            if (_resendCountdown.value <= 0 && (challengeType == ChallengeType.OTP)) {
                startResendTimer()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun startResendTimer() {
        timerJob?.cancel()
        val now = Clock.System.now().toEpochMilliseconds()
        _resendCountdown.value = resendIntervalSeconds
        state = state.copy(
            resendCountdown = resendIntervalSeconds,
            lastRequestTimestamp = now
        )
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_resendCountdown.value > 0) {
                delay(1000.milliseconds)
                _resendCountdown.update { (it - 1).coerceAtLeast(0) }
            }
            state = state.copy(resendCountdown = 0, lastRequestTimestamp = 0)
        }
    }

    fun onCodeChanged(code: String) {
        actionConfirmationManager.optCodeChanged(code)
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
        if (loading) _error.value = null
    }

    fun setError(message: String?) {
        _error.value = message
        _isLoading.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun onVerified() {
        timerJob?.cancel()
        _resendCountdown.value = 0
        _isLoading.value = false
        state = state.copy(isVerified = true, resendCountdown = 0, lastRequestTimestamp = 0)
        actionConfirmationManager.optCodeChanged(null)
    }

    fun clear() {
        timerJob?.cancel()
        state = OtpVerificationState()
        _resendCountdown.value = 0
        _error.value = null
        _isLoading.value = false
        actionConfirmationManager.optCodeChanged(null)
    }

    val formattedTimer: String
        get() {
            val totalSeconds = _resendCountdown.value
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }

    fun cancel() {
        timerJob?.cancel()
    }
}

/**
 * Backward compatibility type alias preserving all existing ViewModel references.
 */
typealias OtpHandler = MfaAuthManager
