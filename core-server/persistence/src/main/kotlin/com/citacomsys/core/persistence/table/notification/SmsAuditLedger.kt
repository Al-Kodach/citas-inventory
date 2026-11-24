package com.citacomsys.core.persistence.table.notification

import com.citacomsys.types.SmsBillMode
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object SmsAuditLedger : Table("sms_audit_ledger") {
    val messageId = varchar("message_id", 225)
    val organizationId = uuid("organization_id")
    val workspaceId = uuid("workspace_id").nullable()
    val userId = uuid("user_id")
    val status = varchar("status", 50)
    val billedFrom = enumerationByName<SmsBillMode>("billed_from", 20).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(messageId)
}
