package com.citacomsys.core.persistence.table.notification

import com.citacomsys.types.JobStatus
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object BackgroundJobs : UUIDTable("background_jobs") {
    val jobType = varchar("job_type", 255)
    val payload = text("payload") // data / pointer
    val status = enumerationByName<JobStatus>("status", 50)
    val priority = integer("priority").default(0)
    val correlationId = varchar("correlation_id", 255).nullable()
    val attempts = integer("attempts").default(0)
    val maxAttempts = integer("max_attempts").default(5)
    val lastAttemptAt = timestamp("last_attempt_at").nullable()
    val scheduledAt = timestamp("scheduled_at").defaultExpression(CurrentTimestamp)
    val lastError = text("last_error").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
