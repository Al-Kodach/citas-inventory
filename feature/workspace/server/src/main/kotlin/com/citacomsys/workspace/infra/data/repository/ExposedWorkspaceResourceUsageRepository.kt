package com.citacomsys.workspace.infra.data.repository

import com.citacomsys.api.workspace.repository.WorkspaceResourceUsageRepository
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.tenant.*
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.kernel.model.*
import com.citacomsys.types.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import kotlin.time.Clock

internal class ExposedWorkspaceResourceUsageRepository: WorkspaceResourceUsageRepository {
    override suspend fun getWorkspaceResourceAllocation(workspaceId: String): WorkspaceResource? =
        dbQuery {
            val modules = WorkspaceModules.selectAll()
                .where { WorkspaceModules.workspaceId eq workspaceId.toUuid() }
                .map { it[WorkspaceModules.module] }
                .toSet()

            WorkspaceResources.selectAll()
                .where { WorkspaceResources.workspaceId eq workspaceId.toUuid() }
                .singleOrNull()?.toResource(modules)
        }

    override suspend fun saveWorkspaceResourceAllocation(
        workspaceId: String,
        pack: WorkspaceResource
    ) = dbQuery {
        val auditId = workspaceId.toUuid()
        WorkspaceResources.upsert {
            it[this.workspaceId] = auditId
            it[packageId] = pack.packageId
            it[packageName] = pack.name
            it[tier] = pack.tier
            it[maxUsers] = pack.maxUsers
            it[maxDevice] = pack.maxDevice
            it[maxBranches] = pack.maxBranches
            it[maxProductsLimit] = pack.maxProductsLimit
            it[maxActiveBatchesLimit] = pack.maxActiveBatchesLimit
            it[retentionMonths] = pack.historyRetentionMonths
            it[updatedAt] = Clock.System.now()
        }

        Workspaces.update({ Workspaces.id eq auditId }) {
            it[updatedAt] = Clock.System.now()
        }

        // Update Modules
        WorkspaceModules.deleteWhere { WorkspaceModules.workspaceId eq auditId }
        WorkspaceModules.batchInsert(pack.modules) { mod ->
            this[WorkspaceModules.workspaceId] = auditId
            this[WorkspaceModules.module] = mod
        }
        Unit
    }

    // --- Resource Usage Implementation ---

    override suspend fun getResourceUsage(workspaceId: String): WorkspaceResourcesUsage = dbQuery {
        WorkspaceResourceUsageTable.selectAll()
            .where { WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }
            .singleOrNull()?.toUsage() ?: error("Resource usage not found for $workspaceId")
    }

    override suspend fun saveResourceUsage(
        workspaceId: String,
        limitsUsage: WorkspaceResourcesUsage
    ) =
        dbQuery {
            WorkspaceResourceUsageTable.upsert {
                it[this.workspaceId] = workspaceId.toUuid()
                it[productCountUsed] = limitsUsage.productCountUsed
                it[activeBatchesCount] = limitsUsage.activeBatchesCount
                it[deviceCount] = limitsUsage.deviceCount
                it[userCount] = limitsUsage.userCount
                it[updatedAt] = Clock.System.now()
            }
            Unit
        }

    override suspend fun incrementUserCount(workspaceId: String) = dbQuery {
        WorkspaceResourceUsageTable.update({ WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }) {
            it[userCount] = userCount plus 1
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun decrementUserCount(workspaceId: String) = dbQuery {
        WorkspaceResourceUsageTable.update({ WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }) {
            it[userCount] = userCount minus 1
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun incrementBranchCount(workspaceId: String) = dbQuery {
        WorkspaceResourceUsageTable.update({ WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }) {
            it[branchCount] = branchCount plus 1
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun decrementBranchCount(workspaceId: String) = dbQuery {
        WorkspaceResourceUsageTable.update({ WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }) {
            it[branchCount] = branchCount minus 1
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun incrementDeviceCount(workspaceId: String) = dbQuery {
        WorkspaceResourceUsageTable.update({ WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }) {
            it[deviceCount] = deviceCount plus 1
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun decrementDeviceCount(workspaceId: String) = dbQuery {
        WorkspaceResourceUsageTable.update({ WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }) {
            it[deviceCount] = deviceCount minus 1
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun incrementSmsUsageCount(workspaceId: String) = dbQuery {
        WorkspaceResourceUsageTable.update({ WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }) {
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun resetSmsCycle(workspaceId: String) = dbQuery {
        WorkspaceResourceUsageTable.update({ WorkspaceResourceUsageTable.workspaceId eq workspaceId.toUuid() }) {
//            it[cycleStartedAt] = Clock.System.now()
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun updateSmsBalance(workspaceId: String, amount: Int) = dbQuery {
        WorkspaceAccountsTable.update({ WorkspaceAccountsTable.workspaceId eq workspaceId.toUuid() }) {
            it[smsBalance] = amount
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun incrementProductCountLimit(workspaceId: String, count: Int) = dbQuery {
        WorkspaceResources.update({ WorkspaceResources.workspaceId eq workspaceId.toUuid() }) {
            it[maxProductsLimit] = maxProductsLimit plus count
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun incrementActiveBatchesLimit(workspaceId: String, count: Int) = dbQuery {
        WorkspaceResources.update({ WorkspaceResources.workspaceId eq workspaceId.toUuid() }) {
            it[maxActiveBatchesLimit] = maxActiveBatchesLimit plus count
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    private fun ResultRow.toUsage() = WorkspaceResourcesUsage(
        productCountUsed = this[WorkspaceResourceUsageTable.productCountUsed],
        activeBatchesCount = this[WorkspaceResourceUsageTable.activeBatchesCount],
        deviceCount = this[WorkspaceResourceUsageTable.deviceCount],
        userCount = this[WorkspaceResourceUsageTable.userCount],
        branchCount = this[WorkspaceResourceUsageTable.branchCount],
        updatedAt = this[WorkspaceResourceUsageTable.updatedAt]
    )
    private fun ResultRow.toResource(modules: Set<ProductModule>) = WorkspaceResource(
        packageId = this[WorkspaceResources.packageId],
        name = this[WorkspaceResources.packageName],
        tier = this[WorkspaceResources.tier],
        modules = modules,
        maxUsers = this[WorkspaceResources.maxUsers],
        maxDevice = this[WorkspaceResources.maxDevice],
        maxBranches = this[WorkspaceResources.maxBranches],
        maxProductsLimit = this[WorkspaceResources.maxProductsLimit],
        maxActiveBatchesLimit = this[WorkspaceResources.maxActiveBatchesLimit],
        historyRetentionMonths = this[WorkspaceResources.retentionMonths],
        updatedAt = this[WorkspaceResources.updatedAt]
    )
}