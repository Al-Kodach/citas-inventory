package com.citacomsys.feature.organization.di

import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.feature.organization.data.remote.api.KtorOrganizationService
import com.citacomsys.feature.organization.ui.details.WorkspaceDetailsViewModel
import com.citacomsys.feature.organization.ui.hub.OrganizationHubViewModel
import com.citacomsys.feature.organization.ui.monitoring.OrganizationMonitoringViewModel
import com.citacomsys.feature.organization.ui.profile.OrganizationProfileViewModel
import com.citacomsys.feature.organization.ui.registration.OrganizationRegistrationViewModel
import com.citacomsys.feature.organization.ui.workspace.WorkspaceListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val organizationFeatureModule = module {
    single<OrganizationService> { KtorOrganizationService(get()) }
    
    viewModelOf(::OrganizationHubViewModel)
    viewModelOf(::OrganizationRegistrationViewModel)
    viewModelOf(::OrganizationProfileViewModel)
    viewModelOf(::OrganizationMonitoringViewModel)
    viewModelOf(::WorkspaceDetailsViewModel)
    viewModelOf(::WorkspaceListViewModel)
}
