package com.citacomsys.api.workspace.service

import com.citacomsys.kernel.model.*
import com.citacomsys.types.ResourceType
import com.citacomsys.types.SmsBillMode

sealed interface ResourceCheckResult {
    object Available : ResourceCheckResult
    data class Depleted(val resource: String, val message: String) : ResourceCheckResult
}

interface WorkspaceResourceManager {
    suspend fun get(workspaceId: String): WorkspaceResource
    suspend fun getUsage(workspaceId: String): WorkspaceResourcesUsage
    /**
     * Checks if a workspace has enough capacity or balance for a resource.
     */
    suspend fun checkAvailability(
        workspaceId: String,
        type: ResourceType,
        quantity: Int = 1
    ): ResourceCheckResult

    /**
     * Increments usage or decrements balance. Throws exception if unavailable.
     */
    suspend fun consume(workspaceId: String, type: ResourceType, quantity: Int = 1)

    /**
     * Specialized shortcut for SMS to handle the Quota vs Wallet logic
     */
    suspend fun consumeSms(workspaceId: String): SmsBillMode
    suspend fun release(workspaceId: String, type: ResourceType, quantity: Int = 1)
}
