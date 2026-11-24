package com.citacomsys.core.notification

import com.citacomsys.core.notification.config.NotificationConfig
import com.citacomsys.core.persistence.table.notification.BackgroundJobs
import com.citacomsys.core.notification.backgroundJobHandler.BackgroundJobHandler
import com.citacomsys.types.JobStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * The generic "template" engine. It polls the DB and delegates jobs
 * to the appropriate registered handler.
 */

internal class BackgroundJobCoordinator(
    private val handlerRegistry: List<BackgroundJobHandler> = emptyList(),
    private val config: NotificationConfig,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    val handler: Map<String, BackgroundJobHandler>
        get() = handlerRegistry.associateBy { it.id }

    private val logger = KotlinLogging.logger {}
    private var notificationJob: Job? = null

    fun start() {
        logger.info("BackgroundJobCoordinator started with ${handlerRegistry.size} handlers. Polling for jobs...")
        notificationJob = scope.launch {
            while (isActive) {
                try {
                    processPendingNotifications()
                } catch (e: Exception) {
                    logger.error(e) { "Error in notification processing loop." }
                }
                delay(5000.milliseconds)
            }
        }
    }

    fun stop() {
        notificationJob?.cancel()
        notificationJob = null
        logger.info { "BackgroundJobCoordinator stopped." }
    }

    private fun processPendingNotifications() {
        val jobsToProcess = findAndLockJobs()

        if (jobsToProcess.isEmpty()) {
            return
        }

        logger.info { "Processing ${jobsToProcess.size} notification jobs." }

        jobsToProcess.forEach { job ->
            scope.launch {
                // Find the correct handler for this job's category.
                val handler = handler[job.handlerId]

                if (handler == null) {
                    logger.error { "No handler found for notification category '${job.handlerId}'. Marking job ${job.id} as failed permanently." }
                    handleFailure(job, maxAttempts = 0) // Fail permanently
                    return@launch
                }

                try {
                    handler.execute(job.payload)
                    markAsCompleted(job.id)
                } catch (e: NotificationException) {
                    logger.info { "Handler requested a state transition for job ${job.id}: ${e.message}" }
                    handleFailure(
                        job,
                        newPayload = e.newPayload,
                        error = e.message
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Handler failed unexpectedly for job ${job.id}. Will retry with same payload." }
                    handleFailure(job, error = e.message)
                }
            }
        }
    }

    private fun handleFailure(
        job: BackgroundJob,
        maxAttempts: Int = config.maxRetries,
        newPayload: String? = null,
        error: String? = null
    ) = transaction {
        BackgroundJobs.update({ BackgroundJobs.id eq job.id }) {
            it[attempts] = job.attempts + 1
            it[this.maxAttempts] = maxAttempts
            it[lastAttemptAt] = Clock.System.now()
            it[updatedAt] = Clock.System.now()
            it[lastError] = error
            if (newPayload != null) {
                it[payload] = newPayload
                it[status] = JobStatus.TRANSITION
            } else {
                it[status] = JobStatus.FAILED
            }
        }
    }

    private fun markAsCompleted(id: UUID) = transaction {
        BackgroundJobs.update({ BackgroundJobs.id eq id }) {
            it[status] = JobStatus.COMPLETED
            it[updatedAt] = Clock.System.now()
        }
    }

    private fun findAndLockJobs(): List<BackgroundJob> = transaction {
        val now = Clock.System.now()
        val retryDelayMinutes = 5L
        val maxAttempts = config.maxRetries

        val eligibleForRetry =
            ((BackgroundJobs.status eq JobStatus.FAILED) or
                    (BackgroundJobs.status eq JobStatus.TRANSITION)) and
                    (BackgroundJobs.attempts less maxAttempts) and
                    (BackgroundJobs.lastAttemptAt neq null) and
                    (BackgroundJobs.lastAttemptAt less now.minus(
                        retryDelayMinutes.toDuration(
                            DurationUnit.MINUTES
                        )
                    ))

        val jobs = BackgroundJobs.selectAll()
            .where { (BackgroundJobs.status eq JobStatus.PENDING) or eligibleForRetry }
            .orderBy(BackgroundJobs.priority to SortOrder.DESC, BackgroundJobs.createdAt to SortOrder.ASC)
            .limit(10)
            .forUpdate(ForUpdateOption.ForUpdate)
            .map { it.toJobRecord() }

        if (jobs.isNotEmpty()) {
            val ids = jobs.map { it.id }
            BackgroundJobs.update({ BackgroundJobs.id inList ids }) {
                it[status] = JobStatus.PROCESSING
                it[updatedAt] = now
            }
        }
        jobs
    }

    private fun ResultRow.toJobRecord() = BackgroundJob(
        id = this[BackgroundJobs.id].value,
        handlerId = this[BackgroundJobs.jobType],
        payload = this[BackgroundJobs.payload],
        status = this[BackgroundJobs.status],
        attempts = this[BackgroundJobs.attempts],
        lastAttemptAt = this[BackgroundJobs.lastAttemptAt],
        createdAt = this[BackgroundJobs.createdAt],
        updatedAt = this[BackgroundJobs.updatedAt]
    )
}