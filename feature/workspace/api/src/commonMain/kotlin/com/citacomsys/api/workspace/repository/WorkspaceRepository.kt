package com.citacomsys.api.workspace.repository

import com.citacomsys.api.workspace.model.Workspace
import com.citacomsys.api.workspace.model.WorkspaceAccount
import com.citacomsys.api.workspace.model.WorkspaceSubscription
import com.citacomsys.types.Address
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.PackageTier
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.ProductType
import com.citacomsys.types.WorkspaceStatus

interface WorkspaceRepository {
    // Workspace Methods
    suspend fun get(workspaceId: String): Result<Workspace>
    suspend fun findByIdentifier(identifier: String): Result<Workspace>
    suspend fun updateStatus(
        workspaceId: String,
        status: WorkspaceStatus,
        reason: String? = null
    ): Result<Unit>

    suspend fun findDuplicate(orgId: String, type: ProductType, name: String): Workspace?

    suspend fun create(
        organizationId: String,
        name: String,
        packageTier: PackageTier,
        businessType: ProductType,
        productKey: String,
        deploymentMode: DeploymentMode,
        businessCode: String,
        address: Address,
        email: String?,
        phone: PhoneNumber?,
        status: WorkspaceStatus
    ): Result<Workspace>

    // Branding Methods (Workspace Scoped)
    suspend fun findBrand(workspaceId: String): BrandingConfig?
    suspend fun saveBrand(workspaceId: String, brandingConfig: BrandingConfig)
    suspend fun deleteBranding(workspaceId: String)

    // Account Methods
    suspend fun getAccount(workspaceId: String): WorkspaceAccount
    suspend fun saveAccount(workspaceId: String, account: WorkspaceAccount)
    suspend fun decrementSmsBalance(workspaceId: String)
    suspend fun updateDeployMode(workspaceId: String, deploymentMode: DeploymentMode)

    // Subscription Methods
    suspend fun getSubscription(workspaceId: String): WorkspaceSubscription
    suspend fun saveSubscription(workspaceId: String, subscription: WorkspaceSubscription)

    suspend fun updateIdentity(
        workspaceId: String,
        name: String?,
        email: String?,
        phone: String?
    )
}
