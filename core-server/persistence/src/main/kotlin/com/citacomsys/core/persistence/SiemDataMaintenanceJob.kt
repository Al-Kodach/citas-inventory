package com.citacomsys.core.persistence

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlinx.datetime.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

internal class SiemDataMaintenanceJob : DataMaintenanceJob {
    private val logger = KotlinLogging.logger("SIEM_MAINTENANCE")

    override val identity: String = "SIEM_MAINTENANCE"

    override fun run(scope: CoroutineScope) {
        logger.info { "Initializing SIEM maintenance worker..." }

        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // Calculate wait time until next maintenance window (e.g., 02:00 AM)
                    val delayMs = calculateMillisUntilNextWindow(hour = 2)
                    logger.debug { "Next SIEM purge scheduled in ${delayMs / 1000 / 60} minutes" }
                    delay(delayMs.milliseconds)

                    logger.info { "Executing SIEM log pruning procedure..." }

                    transaction {
                        // Correctly using the Transaction context for native SQL
                        exec("CALL purge_expired_siem_logs();")
                    }

                    logger.info { "SIEM log pruning completed successfully." }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "SIEM maintenance loop encountered an error. Retrying in 1 hour..." }
                    delay(TimeUnit.HOURS.toMillis(1).milliseconds) // Back off on error
                }
            }
        }
    }

    private fun calculateMillisUntilNextWindow(hour: Int): Long {
        val now =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        var nextWindow = LocalDateTime(now.year, now.month, now.day, hour, 0)

        if (now.hour >= hour) {
            // If already past the window today, schedule for tomorrow
            nextWindow = now.toInstant(TimeZone.currentSystemDefault())
                .plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .let { LocalDateTime(it.year, it.month, it.day, hour, 0) }
        }

        return (nextWindow.toInstant(TimeZone.currentSystemDefault()) -
                now.toInstant(TimeZone.currentSystemDefault())).inWholeMilliseconds
    }
}