package com.citacomsys.core.notification.backgroundJobHandler

import com.citacomsys.core.notification.EnqueueRequest
import com.citacomsys.core.notification.BackgroundJobQueue

/**
 * A handler for a specific category of job. This is the "plugin".
 * It is responsible for its own logic, including deserializing its payload.
 */
interface BackgroundJobHandler {
    /**
     * The unique category ID that links a job in the DB to this handler.
     */
    val id: String

    val priority: Int get() = 0

    /**
     * Executes the background job logic.
     * @param payload The JSON payload string from the database.
     * @throws Exception if the job fails and should be retried.
     */
    suspend fun execute(payload: String)
}

fun BackgroundJobHandler.enqueue(
    backgroundJobQueue: BackgroundJobQueue,
    payload: String,
    priority: Int = this.priority,
    correlationId: String? = null
) {
    backgroundJobQueue.enqueue(
        EnqueueRequest(
            id = id,
            payload = payload,
            priority = priority,
            correlationId = correlationId
        )
    )
}
