package com.citacomsys.core.persistence.table.tenant

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object OrganizationAccountsTable : Table("organization_accounts") {
    val organizationId = reference("organization_id", Organizations.id)
    val totalBalance = integer("total_balance").default(0)
    val currencyCode = varchar("currency_code", 3).default("GHS")
    
    val totalWorkstations = integer("total_workstations").default(0)
    val totalActiveBranches = integer("total_active_branches").default(0)
    val totalStaffAccounts = integer("total_staff_accounts").default(0)

    val totalBillingAmount = long("total_billing_amount").default(0)
    val totalRecurringMonthly = long("total_recurring_monthly").default(0)

    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    
    override val primaryKey = PrimaryKey(organizationId)
}
