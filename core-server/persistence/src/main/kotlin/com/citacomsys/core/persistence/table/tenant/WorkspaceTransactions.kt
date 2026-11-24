package com.citacomsys.core.persistence.table.tenant

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp

object WorkspaceTransactions : UUIDTable("workspace_transactions") {
    val workspaceId = uuid("workspace_id").references(Workspaces.id)
    val amount = decimal("amount", 12, 4)
    val currencyCode = varchar("currency_code", 3)
    val paymentReference = varchar("payment_reference", 255).nullable()
    val transactionType = varchar("transaction_type", 50)
    val description = text("description")
    val createdAt = timestamp("created_at")
}
