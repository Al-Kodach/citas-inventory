package com.citacomsys.core.persistence.table.packages

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object SmsPackages : Table("sms_packages") {

    val id = varchar("id", 50)

    override val primaryKey = PrimaryKey(id)

    val name = varchar("name", 255)

    val units = integer("units")

    val price = long("price_amount")

    val currencyCode = varchar("currency_code", 10)

    val campaignId = optReference("campaign_id", PromotionCampaigns)

    val active = bool("active")

    val createdAt = timestamp("created_at")

    val updatedAt = timestamp("updated_at")
}