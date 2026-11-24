package com.citacomsys.kernel.model

import kotlinx.serialization.Serializable

@Serializable
data class BootstrapWorkspaceResponse(
    val config: WorkspaceRuntimeConfig,
    val state: WorkspaceRuntimeState,
    val organizationConfig: OrganizationRuntimeConfig? = null,
    val tailscaleAuthKey: String? = null
)
