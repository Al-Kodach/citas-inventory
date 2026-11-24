package com.citacomsys.feature.telemetry.server.exposed

import com.citacomsys.core.persistence.table.tenant.Workspaces
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object SystemEvents : Table("system_events") {
    val seq = long("seq").autoIncrement()
    val organizationId = uuid("organization_id").nullable()
    val workspaceId = uuid("workspace_id").references(Workspaces.id).nullable()
    val branchId = uuid("branch_id").nullable()
    val entityType = varchar("entity_type", 50)
    val entityId = varchar("entity_id", 50)
    val action = varchar("action", 20)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(seq)
}
