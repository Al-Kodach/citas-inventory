package com.citacomsys.workspace.infra

import com.citacomsys.api.workspace.branch.BranchRepository
import com.citacomsys.api.workspace.repository.*
import com.citacomsys.api.workspace.service.*
import com.citacomsys.workspace.infra.data.repository.*
import com.citacomsys.workspace.service.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module


val workspaceServiceModule = module {
    singleOf(::ExposedWorkspaceRepository) bind WorkspaceRepository::class
    singleOf(::ExposedWorkspaceResourceUsageRepository) bind WorkspaceResourceUsageRepository::class
    singleOf(::ExposedBranchRepository) bind BranchRepository::class

    singleOf(::BranchServiceImpl) bind BranchService::class
    singleOf(::WorkspaceResourceManagerImpl) bind WorkspaceResourceManager::class
    singleOf(::WorkspaceSubscriptionServiceImpl) bind WorkspaceSubscriptionService::class
    singleOf(::WorkspaceProvisioningServiceImpl) bind WorkspaceProvisioningService::class
}
