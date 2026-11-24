package com.citacomsys.inventory.admin.tenant.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.tenant.OrganizationDetails
import com.citacomsys.admin.api.tenant.TenantApiService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminTenantDetailsViewModel(
    notificationService: InAppNotificationService,
    private val api: TenantApiService
) : BaseViewModel(notificationService) {
    private val _organizationDetails =
        MutableStateFlow<UiResult<OrganizationDetails>>(UiResult.Idle)
    val selectedTenantDetails = _organizationDetails.asStateFlow()

    fun selectTenant(orgId: String) {
        viewModelScope.launch {
            _organizationDetails.value = UiResult.Loading
            val details = runCatching { api.getOrganizationDetails(orgId) }
            _organizationDetails.value = details.asUiResult()
        }
    }

    fun clearSelection() {
        _organizationDetails.value = UiResult.Idle
    }

    fun suspendTenant(orgId: String, reason: String) {
        viewModelScope.launch {
            api.suspendOrganization(orgId, reason).onSuccess {
                showSuccess("Organization suspended successfully")
                selectTenant(orgId)
            }.onFailure {
                showError(it.message ?: "Failed to suspend organization")
            }
        }
    }

    fun activateTenant(orgId: String) {
        viewModelScope.launch {
            api.activateOrganization(orgId).onSuccess {
                showSuccess("Organization is now active")
                selectTenant(orgId)
            }.onFailure {
                showError(it.message ?: "Failed to activateServices organization")
            }
        }
    }

    fun deleteTenant(orgId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            api.suspendOrganization(orgId, "Administrative deletion request").onSuccess {
                showSuccess("Organization suspended / archived successfully")
                onDeleted()
            }.onFailure {
                showError(it.message ?: "Failed to delete organization")
            }
        }
    }

    fun suspendWorkspace(orgId: String, workspaceId: String, reason: String) {
        viewModelScope.launch {
            api.suspendWorkspace(orgId, workspaceId, reason).onSuccess {
                showSuccess("Workspace suspended successfully")
                selectTenant(orgId)
            }.onFailure {
                showError(it.message ?: "Failed to suspend workspace")
            }
        }
    }

    fun activateWorkspace(orgId: String, workspaceId: String) {
        viewModelScope.launch {
            api.activateWorkspace(orgId, workspaceId).onSuccess {
                showSuccess("Workspace activated successfully")
                selectTenant(orgId)
            }.onFailure {
                showError(it.message ?: "Failed to activateServices workspace")
            }
        }
    }

    fun deleteWorkspace(orgId: String, workspaceId: String) {
        viewModelScope.launch {
            api.deleteWorkspace(orgId, workspaceId).onSuccess {
                showSuccess("Workspace deleted successfully")
                selectTenant(orgId)
            }.onFailure {
                showError(it.message ?: "Failed to delete workspace")
            }
        }
    }
}
