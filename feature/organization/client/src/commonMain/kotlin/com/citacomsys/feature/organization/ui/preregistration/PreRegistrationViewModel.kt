package com.citacomsys.feature.organization.ui.preregistration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

data class PreRegistrationState(
    val orgName: String = "",
    val businessCode: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val successRecord: PreRegistrationResult? = null,
    val errorMessage: String? = null
)

data class PreRegistrationResult(
    val organizationId: String,
    val businessCode: String,
    val ownerUsername: String
)

class PreRegistrationViewModel : ViewModel() {
    private val _state = MutableStateFlow(PreRegistrationState())
    val state: StateFlow<PreRegistrationState> = _state.asStateFlow()

    fun updateOrgName(value: String) { _state.update { it.copy(orgName = value) } }
    fun updateBusinessCode(value: String) { _state.update { it.copy(businessCode = value) } }
    fun updateOwnerName(value: String) { _state.update { it.copy(ownerName = value) } }
    fun updateOwnerPhone(value: String) { _state.update { it.copy(ownerPhone = value) } }
    fun updateOwnerEmail(value: String) { _state.update { it.copy(ownerEmail = value) } }
    fun updateUsername(value: String) { _state.update { it.copy(username = value) } }

    fun submitPreRegistration() {
        val current = _state.value
        if (current.orgName.isBlank() || current.businessCode.isBlank() || current.username.isBlank()) {
            _state.update { it.copy(errorMessage = "Please fill in all mandatory fields (Org Name, Business Code, Username).") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                delay(600.milliseconds)
                val mockOrgId = "org_${current.businessCode.lowercase()}_${Random.nextInt(1000, 9999)}"
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        successRecord = PreRegistrationResult(
                            organizationId = mockOrgId,
                            businessCode = current.businessCode.uppercase(),
                            ownerUsername = current.username
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Pre-registration failed.") }
            }
        }
    }

    fun reset() {
        _state.value = PreRegistrationState()
    }

}
