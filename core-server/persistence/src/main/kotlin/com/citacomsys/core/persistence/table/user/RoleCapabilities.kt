package com.citacomsys.core.persistence.table.user

import com.citacomsys.types.UserCapability
import org.jetbrains.exposed.v1.core.Table

object RoleCapabilities : Table("role_capabilities") {
    val id = uuid("id")
    val orgId = uuid("organization_id").index()
    val roleName = varchar("role_name", 50)
    val capability = enumerationByName("capability", 50, UserCapability::class)

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex("idx_role_cap_unique", orgId, roleName, capability)
    }
}