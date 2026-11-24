package com.citacomsys.core.notification

/**
 * A special exception used by NotificationHandlers to signal a state transition.
 * When a handler throws this, it's telling the BackgroundJobCoordinator to
 * update the job's payload and schedule it for a retry, rather than treating
 * it as a hard failure.
 *
 * @param message A descriptive message for logging.
 * @param newPayload The new JSON string payload to save for the next attempt.
 */
class NotificationException(
    override val message: String,
    val newPayload: String? = null
) : Exception(message)
