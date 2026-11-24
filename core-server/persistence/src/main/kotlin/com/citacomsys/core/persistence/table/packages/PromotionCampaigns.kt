package com.citacomsys.core.persistence.table.packages

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp

object PromotionCampaigns : UUIDTable("promotion_campaigns") {

    val code = varchar("code", 100).uniqueIndex()

    val name = varchar("name", 255)

    val description = text("description").nullable()

    val active = bool("active")

    val startsAt = timestamp("starts_at")

    val expiresAt = timestamp("expires_at").nullable()

    val createdAt = timestamp("created_at")

    val updatedAt = timestamp("updated_at")
}

object Promotions : UUIDTable("promotions") {

    val campaignId =
        reference(
            "campaign_id",
            PromotionCampaigns
        )

    val code = varchar("code", 100)

    val name = varchar("name", 255)

    val type = varchar("promotion_type", 50)

    val value = double("value")
    val expiresAt = timestamp("expires_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}