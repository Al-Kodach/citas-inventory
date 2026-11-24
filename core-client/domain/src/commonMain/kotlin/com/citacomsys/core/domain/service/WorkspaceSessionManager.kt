package com.citacomsys.core.domain.service

import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.ProductType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceSessionConfig(
    val workspaceId: String,
    val writeBranchId: String,
    val deploymentMode: DeploymentMode,
    val viewBranchId: String?,
    val productType: ProductType,
    val isBranchLocked: Boolean
)

/**
 * Manages active workspace session contexts, branch constraints, and workspace validation.
 */
interface WorkspaceSessionManager {
    val deploymentMode: DeploymentMode?
    val activeWorkspaceId: String?
    val activeWorkspaceIdFlow: StateFlow<String?>
    val writeTargetBranchId: StateFlow<String?>
    val activeViewBranchId: StateFlow<String?>

    /**
     * The branch lock state of the active workspace
     */
    val isBranchLocked: StateFlow<Boolean>

    /**
     * Use to select the product extension view
     */
    val activeProductType: ProductType get() = ProductType.INVENTORY


    /**
     * Use to perform management task for other branch if the user is a ws manager
     * or has binding role set to all branches
     */
    fun setWriteBranchId(branchId: String)

    fun setViewBranchId(branchId: String?)

    /**
     * Data or Resource management
     * Lock device to only sync data of this branch and also prevent the user from accessing other branches
     */
    fun setBranchLocked(locked: Boolean)

    /**
     * The initial ws session configuration
     * Default branch to resetWorkspaceSession the ws to its initial configuration
     */
    suspend fun configureWorkspace(
        workspaceId: String,
        defaultBranchId: String,
        type: ProductType,
        isBranchLocked: Boolean
    )


    fun resetWorkspaceSession()

    /**
     * Clear active session configuration (workspace logout)
     */
    fun clearWorkspaceSession()
}
