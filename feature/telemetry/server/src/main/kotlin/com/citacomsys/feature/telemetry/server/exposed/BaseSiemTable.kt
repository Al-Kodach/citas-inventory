package com.citacomsys.feature.telemetry.server.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

abstract class BaseSiemTable(name: String) : Table(name) {
    val id = long("id").autoIncrement()
    val requestId = varchar("request_id", 50)
    val workspaceId = uuid("workspace_id").nullable()
    val organizationId = uuid("organization_id").nullable()
    val branchId = uuid("branch_id").nullable()
    val deviceId = varchar("device_id", 255).nullable()
    val productType = varchar("product_type", 50)
    val level = varchar("level", 20)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
