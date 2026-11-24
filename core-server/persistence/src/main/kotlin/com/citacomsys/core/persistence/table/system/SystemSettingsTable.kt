package com.citacomsys.core.persistence.table.system

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object SystemSettingsTable : Table("system_settings") {
    val id = varchar("id", 50).default("GLOBAL_SYSTEM")
    val momoCollectionNumberEncrypted = text("momo_collection_number_encrypted").nullable()
    val momoCollectionName = varchar("momo_collection_name", 255)
    val supportPhoneNumberEncrypted = text("support_phone_number_encrypted").nullable()
    val maintenanceWindowEnabled = bool("maintenance_window_enabled").default(false)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
