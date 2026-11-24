package com.citacomsys.core.persistence.table.user

import com.citacomsys.core.persistence.table.tenant.Workspaces
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object UserWorkspaceBindings : Table("user_workspace_bindings") {
    val id = uuid("id")
    val userId = reference("user_id", Users.id)
    val workspaceId = reference("workspace_id", Workspaces.id)
    val branchId = uuid("branch_id").nullable().index()
    val role = varchar("role", 50)
    val capabilities = text("capabilities").default("")
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
