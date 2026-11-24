package com.citacomsys.feature.organization.ui.registration

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.api.organization.model.OwnerRegistrationDetails
import com.citacomsys.api.organization.model.OrganizationRegistrationRequest
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.security.AuthService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.security.sudo.OtpHandler
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.device.service.DeviceService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.PhoneNumber
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import eu.anifantakis.lib.ksafe.getStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

enum class RegistrationStep {
    WELCOME,
    ORG_INFO,
    BRANDING,
    OWNER_INFO,
    VERIFICATION,
    COMPLETED
}

@Serializable
data class OrgRegistrationDraft(
    val organizationName: String = "Citacom System Ltd",
    val ownerName: String = "Citacom Systems",
    val ownerPhone: String = "0208169630",
    val username: String = "citacom",
    val ownerEmail: String = "citacomsys@gmail.com",
    val ownerPassword: String = "Code4863",
    val branding: BrandingConfig? = null
)

class OrganizationRegistrationViewModel(
    private val api: OrganizationService,
    private val deviceService: DeviceService,
    notification: InAppNotificationService,
    private val prefs: KSafe,
    actionConfirmationManager: ActionConfirmationManager,
    private val platformRuntimeStore: PlatformRuntimeStore,
    private val authService: AuthService
) : BaseViewModel(notification) {

    private val draftKey = "org_registration_draft"
    private val resultKey = "org_registration_result"

    val draft: StateFlow<OrgRegistrationDraft> = prefs.getStateFlow(
        draftKey,
        OrgRegistrationDraft(),
        viewModelScope
    )

    val registrationResult: StateFlow<OrganizationRegistrationResult?> =
        prefs.getStateFlow(resultKey, null, viewModelScope)

    private val _uiState = MutableStateFlow<UiResult<OrganizationRegistrationResult>>(UiResult.Idle)
    val uiState = _uiState.asStateFlow()

    private val _onboardState by prefs.asMutableStateFlow<RegistrationStep>(
        RegistrationStep.ORG_INFO,
        viewModelScope
    )
    val onboardingState = _onboardState.asStateFlow()

    val otpHandler = OtpHandler(viewModelScope, prefs, actionConfirmationManager)

    init {
        // Resume UI state if we already have a result from a previous run
        viewModelScope.launch {
            _onboardState.collect { state ->
                val res = registrationResult.value
                if (state == RegistrationStep.VERIFICATION && registrationResult.value != null) {
                    // If we have a result, we should be ready for OTP
                    otpHandler.setup(
                        title = "Verify Owner",
                        description = "Please enter the code sent to",
                        identifierMasked = res!!.ownerPhoneMasked
                    )
                }
            }
        }
    }

    fun updateDraft(block: (OrgRegistrationDraft) -> OrgRegistrationDraft) {
        viewModelScope.launch {
            prefs.put(draftKey, block(draft.value))
        }
    }

    fun submit() {
        viewModelScope.launch {
            _uiState.value = UiResult.Loading
            val currentDraft = draft.value

            val request = OrganizationRegistrationRequest(
                organizationName = currentDraft.organizationName,
                owner = OwnerRegistrationDetails(
                    name = currentDraft.ownerName,
                    username = currentDraft.username,
                    email = currentDraft.ownerEmail,
                    phone = PhoneNumber(currentDraft.ownerPhone),
                    password = currentDraft.ownerPassword
                ),
                brand = currentDraft.branding,
                deviceId = deviceService.getDeviceId()
            )

            runCatching { api.registerOrganization(request) }.fold(
                onSuccess = {
                    prefs.put(resultKey, it)
                    _uiState.value = UiResult.Success(it)
                    _onboardState.value = RegistrationStep.VERIFICATION
                },
                onFailure = { _uiState.value = it.asUiError() }
            )
        }
    }

    fun navigateTo(state: RegistrationStep) {
        _onboardState.value = state
    }

    fun verify(code: String) {
        val currentResult = registrationResult.value
        if (currentResult == null) {
            otpHandler.setError("Invalid verification state")
            return
        }
        viewModelScope.launch {
            otpHandler.setLoading(true)
            runCatching { api.verifyOwner(currentResult.ownerId, code) }
                .fold(
                    onSuccess = { result ->
                        platformRuntimeStore.saveOrganizationConfig(result.config)
                        authService.enrollAuthUser(
                            result.tokens,
                            result.user,
                            draft.value.ownerPassword
                        )
                        // Clear the draft after successful verification
                        prefs.delete(draftKey)
                        prefs.put(resultKey, null)
                        delay(3000.milliseconds) // wait until flash
                        otpHandler.onVerified()
                        _onboardState.value = RegistrationStep.COMPLETED
                    },
                    onFailure = {
                        otpHandler.setError(it.message ?: "Verification failed")
                    }
                )
        }
    }

    fun reset() {
        viewModelScope.launch {
            prefs.delete(resultKey)
            _uiState.value = UiResult.Idle
            _onboardState.value = RegistrationStep.WELCOME
        }
    }
}
