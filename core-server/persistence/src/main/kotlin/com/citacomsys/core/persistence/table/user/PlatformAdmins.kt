package com.citacomsys.core.persistence.table.user

import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.types.UserStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object PlatformAdmins : Table("platform_admins") {
    val id = uuid("id")
    val name = varchar("name", 100)
    val username = varchar("username", 100).uniqueIndex()
    val phone = varchar("phone", 64).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = text("password_hash")
    val role = varchar("role", 50)
    val status = enumerationByName("status", 50, UserStatus::class).default(UserStatus.ACTIVE)

    // THE SINGLE DEVICE ENFORCEMENT KEY
    val activeHardwareId = varchar("active_hardware_id", 255)
        .references(HardwareRegistry.hardwareId).nullable()

    val mfaEnabled = bool("mfa_enabled").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
