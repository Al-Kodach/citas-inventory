package com.citacomsys.feature.user.server.adaptor

import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.user.RoleCapabilities
import com.citacomsys.core.persistence.table.user.RoleInheritance
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.inventory.api.user.RoleInheritanceDto
import com.citacomsys.inventory.api.user.RoleRepository
import com.citacomsys.types.UserCapability
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

internal class ExposedRoleRepository : RoleRepository {

    override suspend fun save(
        orgId: String,
        role: String,
        capabilities: Set<UserCapability>
    ): String = dbQuery {
        val tenantUuid = orgId.toUuid()

        RoleCapabilities.deleteWhere {
            (RoleCapabilities.roleName eq role) and (RoleCapabilities.orgId eq tenantUuid)
        }

        capabilities.forEach { cap ->
            RoleCapabilities.upsert {
                it[id] = UUID.randomUUID()
                it[this.orgId] = tenantUuid
                it[this.roleName] = role
                it[capability] = cap
            }
        }
        role
    }

    override suspend fun findCapabilities(
        orgId: String,
        role: String
    ): Set<UserCapability> = dbQuery {
        val resolvedCaps = mutableSetOf<UserCapability>()
        val processedRoles = mutableSetOf<String>()
        val rolesToProcess = mutableListOf(role)

        val tenantUuid = orgId.toUuid()

        while (rolesToProcess.isNotEmpty()) {
            val currentRole = rolesToProcess.removeAt(0)
            if (currentRole in processedRoles) continue
            processedRoles.add(currentRole)

            val roleVariants = listOf(
                currentRole,
                "TENANT_$currentRole",
                "PLATFORM_$currentRole"
            ).distinct()

            var directCaps = RoleCapabilities.select(RoleCapabilities.capability)
                .where {
                    (RoleCapabilities.roleName inList roleVariants) and
                            (RoleCapabilities.orgId eq tenantUuid)
                }
                .map { it[RoleCapabilities.capability] }

            if (directCaps.isEmpty()) {
                directCaps = RoleCapabilities.select(RoleCapabilities.capability)
                    .where {
                        (RoleCapabilities.roleName inList roleVariants) and
                                (RoleCapabilities.orgId eq SystemActors.SYSTEM_workspace_id)
                    }
                    .map { it[RoleCapabilities.capability] }
            }
            resolvedCaps.addAll(directCaps)

            var parents = RoleInheritance.select(RoleInheritance.parentRoleName)
                .where {
                    (RoleInheritance.roleName inList roleVariants) and
                            (RoleInheritance.orgId eq tenantUuid)
                }
                .map { it[RoleInheritance.parentRoleName] }

            if (parents.isEmpty()) {
                parents = RoleInheritance.select(RoleInheritance.parentRoleName)
                    .where {
                        (RoleInheritance.roleName inList roleVariants) and
                                (RoleInheritance.orgId eq SystemActors.SYSTEM_workspace_id)
                    }
                    .map { it[RoleInheritance.parentRoleName] }
            }
            rolesToProcess.addAll(parents)
        }

        resolvedCaps
    }

    override suspend fun findAllCapabilities(orgId: String): Map<String, Set<UserCapability>> =
        dbQuery {
            val tenantUuid = orgId.toUuid()

            val roles = RoleCapabilities.select(RoleCapabilities.roleName)
                .where {
                    (RoleCapabilities.orgId eq tenantUuid) or
                            (RoleCapabilities.orgId eq SystemActors.SYSTEM_workspace_id)
                }
                .map { it[RoleCapabilities.roleName] }
                .distinct()

            roles.associateWith { role ->
                findCapabilities(orgId, role)
            }
        }

    override suspend fun saveInheritance(
        orgId: String,
        role: String,
        parentRole: String
    ): Result<Unit> = runCatching {
        dbQuery {
            RoleInheritance.upsert {
                it[id] = UUID.randomUUID()
                it[this.orgId] = orgId.toUuid()
                it[this.roleName] = role
                it[this.parentRoleName] = parentRole
            }
        }
    }

    override suspend fun deleteInheritance(
        orgId: String,
        role: String,
        parentRole: String
    ): Result<Unit> = runCatching {
        dbQuery {
            RoleInheritance.deleteWhere {
                (RoleInheritance.orgId eq orgId.toUuid()) and
                        (RoleInheritance.roleName eq role) and
                        (RoleInheritance.parentRoleName eq parentRole)
            }
        }
    }

    override suspend fun findInheritance(orgId: String): List<RoleInheritanceDto> =
        dbQuery {
            RoleInheritance.selectAll()
                .where { RoleInheritance.orgId eq orgId.toUuid() }
                .map {
                    RoleInheritanceDto(
                        roleName = it[RoleInheritance.roleName],
                        parentRoleName = it[RoleInheritance.parentRoleName]
                    )
                }
        }
}
