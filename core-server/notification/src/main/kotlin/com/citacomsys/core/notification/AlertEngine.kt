package com.citacomsys.core.notification

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.koin.ktor.plugin.koin

class AlertEngine(
    private val alertOrchestrator: DailyAlertOrchestrator,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val logger = LoggerFactory.getLogger(AlertEngine::class.java)
    private var job: Job? = null

    fun start() {
        if (job != null && job?.isActive == true) {
            logger.warn("AlertEngine background daemon is already running.")
            return
        }

        logger.info("Initializing persistent AlertEngine background daemon...")
        job = scope.launch {
            // Initial delay on startup to let server fully initialize
            delay(1.minutes)
            while (isActive) {
                try {
                    logger.info("Executing periodic automated alert pipeline cycle...")
                    alertOrchestrator.processDailyAlertPipeline()
                    logger.info("Automated alert pipeline cycle completed successfully.")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error executing automated alert pipeline cycle", e)
                }

                // Run every 24 hours with crash-resistant background persistence
                delay(24.hours)
            }
        }
    }

    fun stop() {
        logger.info("Stopping AlertEngine background daemon...")
        job?.cancel()
    }
}

fun Application.configureAutomatedAlertWorkers() {
    val alertEngine = koin().get<AlertEngine>()

    monitor.subscribe(ApplicationStarted) {
        environment.log.info("Server is started, initializing automated alert engine workers...")
        alertEngine.start()
    }

    monitor.subscribe(ApplicationStopping) {
        environment.log.info("Server is stopped, shutting down automated alert engine workers...")
        alertEngine.stop()
    }
}
