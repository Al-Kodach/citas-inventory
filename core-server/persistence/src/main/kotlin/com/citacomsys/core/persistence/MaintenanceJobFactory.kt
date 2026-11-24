package com.citacomsys.core.persistence

import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging

/**
 * Orchestrates all background maintenance tasks for the Persistence layer.
 */
class MaintenanceJobFactory(
    private val jobs: List<DataMaintenanceJob>
) {
    private val logger = KotlinLogging.logger("MAINTENANCE_FACTORY")

    fun startAll(scope: CoroutineScope) {
        logger.info { "Starting ${jobs.size} maintenance workers..." }
        jobs.forEach { job ->
            try {
                job.run(scope)
                logger.info { "Worker [${job.identity}] started." }
            } catch (e: Exception) {
                logger.error(e) { "Failed to start worker [${job.identity}]" }
            }
        }
    }
}