package com.citacomsys.feature.organization.ui.profile

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.kernel.service.PlatformClientService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrganizationProfileViewModel(
    private val api: OrganizationService,
    private val platformService: PlatformClientService,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    val config = platformService.organizationConfig
        .asUiResult()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            UiResult.Loading
        )

    private val _refreshState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val refreshState = _refreshState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _refreshState.value = UiResult.Loading
            platformService.refreshOrganizationConfig()
                .onSuccess { _refreshState.value = UiResult.Idle }
                .onFailure {
                    _refreshState.value = UiResult.Error(it.message ?: "Unknown Error")
                }
        }
    }

    fun updateIdentity(name: String, email: String?, phone: String?) {
        viewModelScope.launch {
            if (name.isBlank()) {
                showError("Company id is required")
                return@launch
            }

            api.updateProfile("", name, email, phone).fold(
                onSuccess = {
                    showSuccess("Company identity updated")
                    loadData()
                },
                onFailure = { showError(it.message ?: "Update failed") }
            )
        }
    }
}
