package com.citacomsys.core.persistence.table.tenant

import com.citacomsys.types.Address
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

object BranchesTable : UUIDTable("branches") {
    val workspaceId = reference("workspace_id", Workspaces.id)
    val name = varchar("name", 255)
    val locationCode = varchar("location_code", 50)
    val address = jsonb<Address>("address", Json.Default, Address.serializer()).nullable()
    val contactPhone = varchar("contact_phone", 64).nullable()
    val email = varchar("email", 255).nullable()
    val website = varchar("website", 255).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex("idx_workspace_branch_code", workspaceId, locationCode)
        uniqueIndex("idx_workspace_branch_name", workspaceId, name)
    }
}
