package com.citacomsys.inventory.admin.tenant.preregistration

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.tenant.AdminPreRegisterRequest
import com.citacomsys.admin.api.tenant.TenantApiService
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.types.PhoneNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminPreRegistrationViewModel(
    private val api: TenantApiService,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    private val _orgName = MutableStateFlow("Citacom System LTD")
    val orgName = _orgName.asStateFlow()

    private val _ownerName = MutableStateFlow("Citacom Systems")
    val ownerName = _ownerName.asStateFlow()

    private val _ownerPhone = MutableStateFlow("0208169630")
    val ownerPhone = _ownerPhone.asStateFlow()

    private val _ownerEmail = MutableStateFlow("citacomsys@gmail.com")
    val ownerEmail = _ownerEmail.asStateFlow()

    private val _username = MutableStateFlow("citacom")
    val username = _username.asStateFlow()

    private val _registrationState = MutableStateFlow<UiResult<OrganizationRegistrationResult>>(UiResult.Idle)
    val registrationState = _registrationState.asStateFlow()

    fun updateOrgName(v: String) { _orgName.value = v }
    fun updateOwnerName(v: String) { _ownerName.value = v }
    fun updateOwnerPhone(v: String) { _ownerPhone.value = v }
    fun updateOwnerEmail(v: String) { _ownerEmail.value = v }
    fun updateUsername(v: String) { _username.value = v }


    fun submit() {
        val name = _orgName.value.trim()
        val user = _username.value.trim()

        if (name.isBlank() || user.isBlank()) {
            showError("Please fill in Organization Name, Username, and Temporary Password.")
            return
        }

        if (_ownerPhone.value.isBlank() || PhoneNumber.tryCreate(_ownerPhone.value.trim()) == null) {
            showError("Please fill in Owner Phone Number.")
            return
        }


        viewModelScope.launch {
            _registrationState.value = UiResult.Loading

            val request = AdminPreRegisterRequest(
                organizationName = name,
                ownerName = _ownerName.value.ifBlank { "Owner" },
                ownerPhone = PhoneNumber(_ownerPhone.value),
                ownerEmail = _ownerEmail.value.trim(),
                username = user
            )

            val result = runCatching { api.preRegisterOrganization(request) }.asUiResult()
            _registrationState.value = result
            if (result is UiResult.Success) {
                showSuccess("Organization pre-registered successfully!")
            }
        }
    }

    fun reset() {
        _orgName.value = ""
        _ownerName.value = ""
        _ownerPhone.value = ""
        _ownerEmail.value = ""
        _username.value = ""
        _registrationState.value = UiResult.Idle
    }
}
