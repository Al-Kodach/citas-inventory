package com.citacomsys.admin.api.model

import com.citacomsys.admin.api.tenant.BranchSummary
import com.citacomsys.api.workspace.model.SubscriptionSummary
import com.citacomsys.api.workspace.model.WorkspaceAccount
import com.citacomsys.types.UserDeviceInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WorkspaceDetails")
data class WorkspaceDetails(
    val appName: String,
    val account: WorkspaceAccount,
    val subscription: SubscriptionSummary,
    val workstations: List<UserDeviceInfo> = emptyList(),
    val branches: List<BranchSummary> = emptyList()
)
