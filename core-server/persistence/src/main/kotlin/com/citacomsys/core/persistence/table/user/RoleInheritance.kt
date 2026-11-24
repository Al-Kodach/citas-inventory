package com.citacomsys.core.persistence.table.user

import org.jetbrains.exposed.v1.core.Table

object RoleInheritance : Table("role_inheritance") {
    val id = uuid("id")
    val orgId = uuid("organization_id")
    val roleName = varchar("role_name", 50)
    val parentRoleName = varchar("parent_role_name", 50)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("idx_role_inheritance_unique", orgId, roleName, parentRoleName)
        index(false, orgId, roleName)
    }
}
