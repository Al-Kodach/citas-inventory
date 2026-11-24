package com.citacomsys.feature.workspace.ui.settings

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.api.workspace.service.WorkspaceApiService
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.BrandingConfig
import kotlinx.coroutines.launch

class WorkspaceBrandingViewModel(
    private val kernelService: PlatformClientService,
    private val workspaceApiService: WorkspaceApiService,
    private val organizationService: OrganizationService,
    private val sessionManager: AppSessionManager,
    private val preferencesRepository: PreferencesRepository,
    private val store: PlatformRuntimeStore,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    val branding = kernelService.branding
    val workspaceConfig = kernelService.activeWorkspaceConfig
    val useOrgBranding = preferencesRepository.useOrgBranding
    val usePlatformBranding = preferencesRepository.usePlatformBranding

    fun setUseOrgBranding(useOrg: Boolean) {
        preferencesRepository.setUseOrgBranding(useOrg)
        if (useOrg) {
            preferencesRepository.setUsePlatformBranding(false)
        }
        showSuccess(if (useOrg) "Using Organization branding" else "Using Workspace branding")
    }

    fun setUsePlatformBranding(usePlatform: Boolean) {
        preferencesRepository.setUsePlatformBranding(usePlatform)
        if (usePlatform) {
            preferencesRepository.setUseOrgBranding(false)
        }
        showSuccess(if (usePlatform) "Using Platform default branding" else "Custom branding active")
    }

    fun updateBranding(config: BrandingConfig?) {
        if (config == null) {
            // Only set org branding if platform branding is not active
            if (!usePlatformBranding.value && !useOrgBranding.value) {
                setUseOrgBranding(true)
            }
            return
        }

        viewModelScope.launch {
            preferencesRepository.setUseOrgBranding(false)
            preferencesRepository.setUsePlatformBranding(false)

            val isOrgMode = sessionManager.workStationInfo.value.isOrgMode
            val orgId = sessionManager.organizationId ?: sessionManager.workStationInfo.value.orgName
            val wsId = workspaceConfig.value?.workspaceId ?: sessionManager.activeWorkspaceId
            val targetId = if (isOrgMode) orgId else (wsId ?: orgId)

            if (targetId.isNotBlank()) {
                store.saveBranding(targetId, config)
            }

            if (isOrgMode) {
                runCatching {
                    organizationService.saveBranding(orgId, config)
                }.onSuccess {
                    showSuccess("Organization appearance updated & synced")
                    viewModelScope.launch {
                        kernelService.refreshOrganizationConfig()
                    }
                }.onFailure {
                    showSuccess("Organization appearance saved locally (offline)")
                }
            } else {
                if (wsId != null) {
                    workspaceApiService.updateBranding(config)
                        .onSuccess {
                            showSuccess("Workspace appearance updated & synced")
                            viewModelScope.launch {
                                kernelService.refreshWorkspaceConfig(wsId)
                            }
                        }
                        .onFailure {
                            showSuccess("Workspace appearance saved locally (offline)")
                        }
                } else {
                    showSuccess("Appearance saved locally")
                }
            }
        }
    }
}
