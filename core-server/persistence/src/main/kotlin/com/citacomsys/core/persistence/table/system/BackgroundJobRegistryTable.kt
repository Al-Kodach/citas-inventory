package com.citacomsys.core.persistence.table.system

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object BackgroundJobRegistryTable : Table("background_job_registry") {
    val jobName = varchar("job_name", 64)
    val intervalMinutes = integer("interval_minutes").default(30)
    val isEnabled = bool("is_enabled").default(true)
    val lastExecutedAt = timestamp("last_executed_at").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(jobName)
}
