package com.citacomsys.module.server.customer.exposedTable

import com.citacomsys.core.persistence.table.tenant.BranchesTable
import com.citacomsys.core.persistence.table.tenant.Organizations
import com.citacomsys.core.persistence.table.tenant.Workspaces
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import java.util.UUID

object CustomerTable : UUIDTable("customers") {
    val workspaceId: Column<UUID> = uuid("workspace_id").references(Workspaces.id)
    val organizationId: Column<UUID> = uuid("organization_id").references(Organizations.id)
    val branchId: Column<UUID> = uuid("branch_id").references(BranchesTable.id)
    val fullName: Column<String> = varchar("full_name", 255)
    val phone: Column<String?> = varchar("phone", 64).nullable()
    val email: Column<String?> = varchar("email", 255).nullable()

    // Legal Compliance & Notification Consent Controls
    val isAlertsConsented: Column<Boolean> = bool("is_alerts_consented").default(true)

    // Financial Core Extension
    val creditLimitAmount: Column<Long?> = long("credit_limit_amount").nullable()
    val creditLimitCurrency: Column<String?> = varchar("credit_limit_currency", 3).nullable()

    val currentBalanceAmount: Column<Long?> = long("current_balance_amount").nullable()
    val currentBalanceCurrency: Column<String?> = varchar("current_balance_currency", 3).nullable()
    val deletedAt = timestamp("deleted_at").nullable()

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    init {
        index(
            customIndexName = "uidx_tenant_customer_phone_partial",
            isUnique = true,
            columns = arrayOf(organizationId, phone),
            filterCondition = { phone.isNotNull() }
        )
        index(
            customIndexName = "uidx_tenant_customer_email_partial",
            isUnique = true,
            columns = arrayOf(organizationId, email),
            filterCondition = { email.isNotNull() }
        )
    }
}

object CustomerLedgerTable : UUIDTable("customer_ledger") {
    val customerId: Column<UUID> = uuid("customer_id").references(CustomerTable.id)
    val amountMinor: Column<Long> = long("amount_minor")
    val currency: Column<String> = varchar("currency", 3)
    val type: Column<String> = varchar("type", 50)
    val method: Column<String?> = varchar("method", 50).nullable()
    val saleId: Column<UUID?> = uuid("sale_id").nullable()
    val reference: Column<String?> = varchar("reference", 255).nullable()
    val recordedAt: Column<Long> = long("recorded_at")
    val note: Column<String?> = text("note").nullable()
}
