package com.citacomsys.core.notification

import com.citacomsys.core.persistence.table.notification.BackgroundJobs
import com.citacomsys.types.JobStatus
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * A generic request to enqueue any background job.
 * @param id A string that matches a BackgroundJobHandler's id.
 * @param payload A serializable data class containing the job's details.
 * @param priority Execution priority (higher number = processed first).
 * @param correlationId Optional trace/correlation ID for logging and debugging.
 */
data class EnqueueRequest<T : Any>(
    val id: String,
    val payload: T,
    val priority: Int = 0,
    val correlationId: String? = null
)

class BackgroundJobQueue {
    /**
     * Enqueues a background job request.
     *
     * If the payload is already a String, it is stored directly (prevents double-encoding).
     * Otherwise, it is serialized to JSON using kotlinx.serialization.
     */
    inline fun <reified T : Any> enqueue(request: EnqueueRequest<T>) {
        val rawPayload = if (request.payload is String) {
            request.payload
        } else {
            Json.encodeToString(request.payload)
        }

        transaction {
            BackgroundJobs.insert {
                it[jobType] = request.id
                it[status] = JobStatus.PENDING
                it[payload] = rawPayload
                it[priority] = request.priority
                it[correlationId] = request.correlationId
            }
        }
    }
}
