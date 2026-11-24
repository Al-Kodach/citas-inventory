package com.citacomsys.core.persistence.table.user

import com.citacomsys.core.persistence.table.tenant.Organizations
import com.citacomsys.core.persistence.table.tenant.Workspaces
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import java.util.UUID

object UserSessions : Table("user_sessions") {
    val id = uuid("id")
    val userId = reference("user_id", Users.id)
    val organizationId = reference("organization_id", Organizations.id)
    val workspaceId = reference("workspace_id", Workspaces.id).nullable()
    val deviceId = varchar("device_id", 255)
    val tokenHash = varchar("token_hash", 225).default("")
    val familyId = uuid("family_id").default(UUID.randomUUID())
    val lastIp = varchar("last_ip", 255).nullable()
    val userAgent = varchar("user_agent", 255).nullable()
    val platform = varchar("platform", 255).nullable()
    val appVersion = varchar("app_version", 255).nullable()
    val isRevoked = bool("is_revoked").default(false)
    val revokedAt = timestamp("revoked_at").nullable()
    val revokeReason = varchar("revoke_reason", 255).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val lastUsedAt = timestamp("last_used_at").defaultExpression(CurrentTimestamp)
    val expiresAt = timestamp("expires_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
