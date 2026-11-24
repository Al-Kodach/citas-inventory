package com.citacomsys.core.persistence.table.tenant

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object WorkspaceAccountsTable : Table("workspace_accounts") {
    val workspaceId = reference("workspace_id", Workspaces.id)
    val organizationId = reference("organization_id", Organizations.id)
    val deploymentMode = varchar("deployment_mode", 50).default("CLOUD_SYNCED")
    val balance = decimal("balance", 12, 4).default(java.math.BigDecimal.ZERO)
    val currencyCode = varchar("currency_code", 3).default("GHS")
    val smsBalance = integer("sms_balance").default(0)
    val paymentProviderCustomerId = varchar("payment_provider_customer_id", 255).nullable()
    val defaultPaymentMethodId = varchar("default_payment_method_id", 255).nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    
    override val primaryKey = PrimaryKey(workspaceId)
}
