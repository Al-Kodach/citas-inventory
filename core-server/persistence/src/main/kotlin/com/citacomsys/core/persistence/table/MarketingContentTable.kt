package com.citacomsys.core.persistence.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object MarketingContentTable : Table("marketing_content") {
    val id = varchar("id", 50)
    val contentType = varchar("content_type", 50)
    val title = varchar("title", 255)
    val subtitle = varchar("subtitle", 255).nullable()
    val description = text("description").nullable()
    val mediaUrl = varchar("media_url", 500).nullable()
    val authorName = varchar("author_name", 255).nullable()
    val authorTitle = varchar("author_title", 255).nullable()
    val orderRank = integer("order_rank").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
