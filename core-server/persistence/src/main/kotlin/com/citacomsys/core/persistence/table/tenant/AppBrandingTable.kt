package com.citacomsys.core.persistence.table.tenant

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object AppBrandingTable : Table("branding") {
    val id = uuid("id").references(Workspaces.id) // orgId or workspaceId
    val appName = varchar("app_name", 225)
    val primaryColor = long("primary_color")
    val accentColor = long("accent_color").nullable().default(null)
    val tertiaryColor = long("tertiary_color").nullable().default(null)
    val errorColor = long("error_color").nullable().default(null)
    val logoUrl = text("logo_url").nullable().default(null)
    val defaultDarkMode = bool("default_dark_mode").default(false)
    val paletteStyle = text("palette_style").nullable().default(null)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
