package com.citacomsys.backup.api.model

import com.citacomsys.types.DeploymentMode
import kotlinx.serialization.Serializable

@Serializable
internal data class BackupWorkspaceInfo(
    val organizationId: String,
    val tenantName: String,
    val deploymentMode: DeploymentMode
)