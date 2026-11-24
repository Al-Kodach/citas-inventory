package com.citacomsys.api.workspace.model

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.kernel.model.BranchSummary
import com.citacomsys.kernel.model.WorkspaceResource
import com.citacomsys.kernel.model.WorkspaceResourcesUsage
import com.citacomsys.types.Address
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.Money
import com.citacomsys.types.PackageTier
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.ProductType
import com.citacomsys.types.WorkspaceStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
@SerialName("Workspace")
data class Workspace(
    val id: String,
    val organizationId: String,
    val businessType: ProductType,
//    val packageTier: PackageTier,
    val name: String,
    val productKey: String,
    val businessCode: String? = null,
    val address: Address?,
    val deploymentMode: DeploymentMode,
    val email: String?,
    val phone: PhoneNumber?,
    val status: WorkspaceStatus,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)


/**
 * Represents the financial account associated with a workspace,
 * belonging to a specific organization.
 */
@Serializable
data class WorkspaceAccount(
    val workspaceId: String,
    val organizationId: String,
    val balance: Money,
    val smsBalance: Int = 0,
    val deploymentMode: DeploymentMode = DeploymentMode.CLOUD_SYNCED,
    val paymentProviderCustomerId: String?,
    val defaultPaymentMethodId: String?,
    val updatedAt: Instant
)

@Serializable
@SerialName("WorkspaceDetails")
data class WorkspaceDetails(
    val workspaceId: String,
    val organizationId: String,
    val name: String,
    val type: ProductType,
    val status: WorkspaceStatus,
    val account: WorkspaceAccount? = null,
    val resource: WorkspaceResource? = null,
    val subscription: WorkspaceSubscription? = null,
    val usage: WorkspaceResourcesUsage? = null,
    val branches: List<BranchSummary> = emptyList()
)





@Serializable
data class WorkspaceRegistrationRequest(
    val name: String,
    val mainBranchName: String,
    val type: ProductType,
    val email: String?,
    val phone: PhoneNumber?,
    val address: Address,
    val branding: BrandingConfig?,
    val pkg: PkgProvisionRequest
)

@Serializable
data class BranchRegistrationRequest(
    val name: String,
    val locationCode: String,
    val address: Address? = null,
    val contactPhone: String? = null
)

@Serializable
data class WorkspaceProvisioningResult(
    val workspaceId: String,
    val productKey: String,
    val businessCode: String
)
