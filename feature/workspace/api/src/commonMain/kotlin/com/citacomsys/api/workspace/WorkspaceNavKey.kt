package com.citacomsys.api.workspace

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkspaceNavKey : NavKey

@Serializable
data object WorkspaceDashboardNav : WorkspaceNavKey

@Serializable
data object WorkspaceProfileNav : WorkspaceNavKey

@Serializable
data object NewWorkspaceNav: WorkspaceNavKey

@Serializable
data object WorkspaceBrandNav: WorkspaceNavKey

@Serializable
data object WorkspaceSelectionNav: WorkspaceNavKey

