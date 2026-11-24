package com.citacomsys.feature.workspace.di

import com.citacomsys.api.workspace.branch.BranchRepository
import com.citacomsys.api.workspace.service.WorkspaceApiService
import com.citacomsys.api.workspace.service.WorkspaceProvisioningApiService
import com.citacomsys.client.customer.ui.widget.CustomerGrowthWidgetViewModel
import com.citacomsys.feature.workspace.data.remote.KtorBranchRepository
import com.citacomsys.feature.workspace.data.remote.api.KtorWorkspaceApiService
import com.citacomsys.feature.workspace.data.remote.api.KtorWorkspaceProvisioningApiService
import com.citacomsys.feature.workspace.ui.branch.BranchManagementViewModel
import com.citacomsys.feature.workspace.ui.dashboard.WorkspaceDashboardViewModel
import com.citacomsys.feature.workspace.ui.profile.WorkspaceProfileViewModel
import com.citacomsys.feature.workspace.ui.settings.WorkspaceBrandingViewModel
import com.citacomsys.feature.workspace.ui.registration.orchestrator.WorkspaceSetupViewModel
import com.citacomsys.feature.workspace.ui.launcher.WorkspaceSelectorViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val workspaceFeatureModule = module {
    single<WorkspaceApiService> { KtorWorkspaceApiService(get()) }
    
    single<BranchRepository> { 
        KtorBranchRepository(
            client = get()
        )
    }

    single<WorkspaceProvisioningApiService> {
        KtorWorkspaceProvisioningApiService(get())
    }

    viewModelOf(::BranchManagementViewModel)
    viewModelOf(::WorkspaceSetupViewModel)
    viewModelOf(::WorkspaceProfileViewModel)
    viewModelOf(::WorkspaceBrandingViewModel)
    viewModelOf(::WorkspaceSelectorViewModel)
    viewModelOf(::CustomerGrowthWidgetViewModel)
    viewModelOf(::WorkspaceDashboardViewModel)
}
