package com.citacomsys.core.persistence.table.tenant

import com.citacomsys.types.BillingCycle
import com.citacomsys.types.SubscriptionStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object WorkspaceSubscriptionsTable : Table("workspace_subscriptions") {
    val workspaceId = reference("workspace_id", Workspaces.id)
    val status = enumerationByName("status", 50, SubscriptionStatus::class)
    val billingCycle = enumerationByName<BillingCycle>("billing_cycle", 100)
    val currencyCode = varchar("currency_code", 3)
    val recurringAmountMinor = long("recurring_amount_minor")
    val lineItemsJson = text("line_items_json")
    val version = integer("version").default(1)

    val startedAt = timestamp("start_date")
    val canceledAt = timestamp("canceled_at").nullable()
    val endedAt = timestamp("end_date")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(workspaceId)
}
