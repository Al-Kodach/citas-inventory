package com.citacomsys.core.persistence.table.tenant

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object Organizations : UUIDTable("organizations") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).nullable()
    val phone = varchar("phone", 64).nullable()
    val businessCode = varchar("business_code", 255).uniqueIndex()
    val ownerId = uuid("owner_id").nullable()
    val fleetDna = varchar("fleet_dna", 100).nullable()
    val status = varchar("status", 50)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
