package com.citacomsys.core.notification.sms.data

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.notification.sms.SmsAudit
import com.citacomsys.core.persistence.table.notification.SmsAuditLedger
import com.citacomsys.core.persistence.transaction.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

internal class ExposedSmsAuditDataSource : SmsAuditDataSource {

    private fun ResultRow.toSmsAudit(): SmsAudit {
        return SmsAudit(
            messageId = this[SmsAuditLedger.messageId],
            organizationId = this[SmsAuditLedger.organizationId].toString(),
            workspaceId = this[SmsAuditLedger.workspaceId].toString(),
            userId = this[SmsAuditLedger.userId].toString(),
            status = this[SmsAuditLedger.status],
            billedFrom = this[SmsAuditLedger.billedFrom]
        )
    }

    override suspend fun save(audit: SmsAudit): Unit = dbQuery {
        SmsAuditLedger.upsert {
            it[messageId] = audit.messageId
            it[organizationId] = audit.organizationId.toUuid()
            it[workspaceId] = audit.workspaceId.toUuid()
            it[userId] = audit.userId.toUuid()
            it[status] = audit.status
            it[billedFrom] = audit.billedFrom
        }
    }

    override suspend fun get(messageId: String): SmsAudit? = dbQuery {
        SmsAuditLedger.selectAll()
            .where { SmsAuditLedger.messageId eq messageId }
            .singleOrNull()?.toSmsAudit()
    }

    override suspend fun getAll(organizationId: String): List<SmsAudit> = dbQuery {
        SmsAuditLedger.selectAll()
            .where { SmsAuditLedger.organizationId eq organizationId.toUuid() }
            .map { it.toSmsAudit() }
    }

    override suspend fun updateStatus(messageId: String, status: String): Unit = dbQuery {
        // Implementation
    }
}
