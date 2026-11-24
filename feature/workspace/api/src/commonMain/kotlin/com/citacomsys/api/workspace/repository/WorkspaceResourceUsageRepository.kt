package com.citacomsys.api.workspace.repository

import com.citacomsys.kernel.model.*

interface WorkspaceResourceUsageRepository {
    suspend fun getWorkspaceResourceAllocation(workspaceId: String): WorkspaceResource?
    suspend fun saveWorkspaceResourceAllocation(workspaceId: String, pack: WorkspaceResource)

    suspend fun getResourceUsage(workspaceId: String): WorkspaceResourcesUsage
    suspend fun saveResourceUsage(workspaceId: String, limitsUsage: WorkspaceResourcesUsage)

    suspend fun incrementUserCount(workspaceId: String)
    suspend fun decrementUserCount(workspaceId: String)

    suspend fun incrementDeviceCount(workspaceId: String)
    suspend fun decrementDeviceCount(workspaceId: String)

    suspend fun incrementBranchCount(workspaceId: String)
    suspend fun decrementBranchCount(workspaceId: String)


    suspend fun updateSmsBalance(workspaceId: String, amount: Int)
    suspend fun incrementSmsUsageCount(workspaceId: String)
    suspend fun resetSmsCycle(workspaceId: String)


    suspend fun incrementProductCountLimit(workspaceId: String, count: Int)
    suspend fun incrementActiveBatchesLimit(workspaceId: String, count: Int)
}