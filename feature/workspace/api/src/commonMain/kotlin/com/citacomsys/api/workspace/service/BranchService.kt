package com.citacomsys.api.workspace.service

import com.citacomsys.api.workspace.branch.Branch

interface BranchService {
    suspend fun save(
        workspaceId: String,
        branch: Branch
    ): Branch

    suspend fun get(workspaceId: String, branchId: String): Branch
    suspend fun getAll(workspaceId: String): List<Branch>

    suspend fun delete(workspaceId: String, branchId: String)
}
