package com.citacomsys.api.workspace.branch


interface BranchRepository {
    suspend fun save(branch: Branch): Unit
    suspend fun getAll(workspaceId: String): List<Branch>
    suspend fun get(workspaceId: String, id: String): Branch
    suspend fun delete(workspaceId: String, id: String)
}
