package com.citacomsys.workspace.infra.data.repository

import com.citacomsys.api.workspace.branch.BranchRepository
import com.citacomsys.api.workspace.branch.Branch
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.tenant.BranchesTable
import com.citacomsys.core.persistence.transaction.dbQuery
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.jdbc.selectAll

internal class ExposedBranchRepository : BranchRepository {

    override suspend fun save(branch: Branch): Unit = dbQuery {
        BranchesTable.upsert {
            it[id] = branch.id.toUuid()
            it[workspaceId] = branch.workspaceId.toUuid()
            it[name] = branch.name
            it[locationCode] = branch.locationCode
            it[address] = branch.address
            it[contactPhone] = branch.contactPhone
            it[email] = branch.email
            it[website] = branch.website
            it[isActive] = branch.isActive
            it[createdAt] = branch.createdAt
            it[updatedAt] = branch.updatedAt
        }
    }

    override suspend fun get(workspaceId: String, id: String): Branch = dbQuery {
        BranchesTable.selectAll()
            .where {
                BranchesTable.id eq id.toUuid() and
                        (BranchesTable.workspaceId eq workspaceId.toUuid())
            }
            .map { it.toBranch() }
            .singleOrNull() ?: throw NotFoundException("Branch not found")
    }

    override suspend fun getAll(workspaceId: String): List<Branch> = dbQuery {
        BranchesTable.selectAll()
            .where {
                BranchesTable.workspaceId eq workspaceId.toUuid()
            }
            .map { it.toBranch() }
    }

    override suspend fun delete(workspaceId: String, id: String): Unit = dbQuery {
        val deleted = BranchesTable.deleteWhere {
            BranchesTable.id eq id.toUuid() and (BranchesTable.workspaceId eq workspaceId.toUuid())
        } == 1
        if (!deleted) throw NotFoundException("Branch not found")
    }

    private fun ResultRow.toBranch(): Branch {
        return Branch(
            id = this[BranchesTable.id].value.toString(),
            workspaceId = this[BranchesTable.workspaceId].toString(),
            name = this[BranchesTable.name],
            locationCode = this[BranchesTable.locationCode],
            address = this[BranchesTable.address],
            contactPhone = this[BranchesTable.contactPhone],
            email = this[BranchesTable.email],
            website = this[BranchesTable.website],
            isActive = this[BranchesTable.isActive],
            createdAt = this[BranchesTable.createdAt],
            updatedAt = this[BranchesTable.updatedAt]
        )
    }
}
