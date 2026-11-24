package com.citacomsys.kernel.service

import com.citacomsys.kernel.model.*
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.ProductModule
import kotlinx.coroutines.flow.StateFlow

/**
 * The contract for the Platform Runtime Kernel.
 * Manages the active workspace configuration, state and licences,
 */
interface PlatformClientService {
    val organizationConfig: StateFlow<OrganizationRuntimeConfig?>

    val activeWorkspaceConfig: StateFlow<WorkspaceRuntimeConfig?>
    val resource: StateFlow<WorkspaceResource?>
    val activeWorkspaceState: StateFlow<WorkspaceRuntimeState?>

    val branding: StateFlow<BrandingConfig?>

    val modulesMap: StateFlow<Map<ProductModule, EffectiveModulePolicy>>

    fun canWrite(module: ProductModule): Boolean

    suspend fun refreshOrganizationConfig(): Result<Unit>
    suspend fun refreshWorkspaceConfig(): Result<Unit>
    suspend fun refreshWorkspaceConfig(workspaceId: String): Result<Unit>
    suspend fun refreshWorkspaceState(): Result<Unit>
}
