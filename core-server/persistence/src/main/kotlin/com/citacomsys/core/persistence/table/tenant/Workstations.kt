package com.citacomsys.core.persistence.table.tenant

import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.HardwareMasterStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object HardwareRegistry : Table("hardware_registry") {
    val hardwareId = varchar("hardware_id", 255)
    val organizationId = uuid("organization_id").references(Organizations.id)
    val masterStatus = enumerationByName<HardwareMasterStatus>(
        "master_status",
        50
    ).default(HardwareMasterStatus.ACTIVE)
    val securityReason = text("security_reason").nullable()
    val fingerprint = varchar("fingerprint", 255).nullable()
    val displayName = varchar("display_name", 255).nullable()
    val publicKey = text("public_key").nullable()
    val model = varchar("model", 255).nullable()
    val osName = varchar("os_name", 100).nullable()
    val architecture = varchar("architecture", 50).nullable()

    val lastIp = varchar("last_ip", 255).nullable()
    val lastUsedAt = timestamp("last_used_at")

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(hardwareId)
}

object WorkstationBindings : Table("workstation_bindings") {
    val id = uuid("id")
    val hardwareId = varchar("hardware_id", 255).references(HardwareRegistry.hardwareId)
    val workspaceId = uuid("workspace_id").references(Workspaces.id)
    val orgId = uuid("organization_id").references(Organizations.id)
    val workspaceName = varchar("workspace_name", 255)
    val displayName = varchar("display_name", 255).nullable()
    val status = enumerationByName<DeviceStatus>("status", 50)
        .default(DeviceStatus.ACTIVE)
    val lastIp = varchar("last_ip", 255).nullable()
    val lastUsedAt = timestamp("last_used_at")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(hardwareId, workspaceId)
    }
}
