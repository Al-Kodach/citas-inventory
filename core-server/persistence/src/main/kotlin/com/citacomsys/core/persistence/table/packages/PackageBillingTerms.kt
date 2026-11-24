package com.citacomsys.core.persistence.table.packages

import com.citacomsys.types.BillingCycle
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object PackageBillingTerms : UUIDTable("package_billing_terms") {
    val packageId = varchar("package_id", 50).references(ProductPackages.id)
    val deploymentMode = varchar("deployment_mode", 20).default("CLOUD_SYNCED")
    val billingCycle = enumerationByName("billing_cycle", 20, BillingCycle::class)
    val priceAmount = long("price_amount")
    val currencyCode = varchar("currency_code", 3).default("GHS")
    val campaignId = uuid("campaign_id").references(PromotionCampaigns.id).nullable()
    val isDefault = bool("is_default").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
