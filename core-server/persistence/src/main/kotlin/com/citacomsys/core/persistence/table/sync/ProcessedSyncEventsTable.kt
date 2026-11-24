package com.citacomsys.core.persistence.table.sync

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object ProcessedSyncEventsTable : Table("processed_sync_events") {
    val eventId = varchar("event_id", 128)
    val workspaceId = uuid("workspace_id")
    val entityType = varchar("entity_type", 64)
    val entityId = varchar("entity_id", 128)
    val httpStatus = integer("http_status")
    val responseBody = text("response_body").nullable()
    val processedAt = timestamp("processed_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(eventId)
}
