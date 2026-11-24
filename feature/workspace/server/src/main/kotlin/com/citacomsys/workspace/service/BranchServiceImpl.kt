package com.citacomsys.workspace.service

import com.citacomsys.api.workspace.branch.Branch
import com.citacomsys.api.workspace.branch.BranchRepository
import com.citacomsys.api.workspace.service.BranchService
import com.citacomsys.api.workspace.service.WorkspaceResourceManager
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.types.EntityType
import com.citacomsys.types.ResourceType

internal class BranchServiceImpl(
    private val branchRepository: BranchRepository,
    private val resourceManager: WorkspaceResourceManager,
    private val auditor: TelemetryAuditor,
) : BranchService {

    override suspend fun save(
        workspaceId: String,
        branch: Branch
    ): Branch {
        val existing = runCatching { branchRepository.get(workspaceId, branch.id) }.getOrNull()
        if (existing != null) {
            resourceManager.consume(workspaceId, ResourceType.BRANCH)
        }

        if (workspaceId == branch.id && branch.isActive.not()) {
            auditor.logApplicationEvent(
                action = "MAIN_BRANCH_DEACTIVATED",
                entityType = EntityType.TENANT.name,
                entityId = branch.id,
                summary = "Branch '${branch.name}' deactivated at location: ${branch.locationCode}",
                level = TelemetryLevel.ERROR
            )
            error("Main Store cannot be deactivated")
        }

        branchRepository.save(branch)

        val action = if (existing != null) "BRANCH_UPDATED" else "BRANCH_CREATED"

        auditor.logApplicationEvent(
            action = action,
            entityType = EntityType.TENANT.name,
            entityId = branch.id,
            summary = "Branch '${branch.name}' established at location: ${branch.locationCode}",
            level = TelemetryLevel.INFO
        )

        return branch
    }

    override suspend fun get(workspaceId: String, branchId: String): Branch {
        return branchRepository.get(workspaceId, branchId)
    }

    override suspend fun getAll(workspaceId: String): List<Branch> {
        return branchRepository.getAll(workspaceId)
    }

    override suspend fun delete(workspaceId: String, branchId: String) {
        branchRepository.delete(workspaceId, branchId)
        resourceManager.release(workspaceId, ResourceType.BRANCH)
    }
}
