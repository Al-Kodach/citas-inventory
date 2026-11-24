package com.citacomsys.core.persistence

import com.citacomsys.core.persistence.config.DbConfig
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.EntityChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import mu.KotlinLogging
import org.postgresql.PGConnection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.text.uppercase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

internal class PostgresNotificationListener(
    private val config: DbConfig
) {
    private val logger = KotlinLogging.logger { }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            logger.info { "Starting Database notification Listener" }

            while (isActive) {
                try {
                    // We need a dedicated connection for LISTEN/NOTIFY
                    DriverManager.getConnection(config.url, config.user, config.pass).use { conn ->
                        val pgConn = conn.unwrap(PGConnection::class.java)

                        conn.createStatement().execute("LISTEN table_changes")
                        logger.info { "Successfully subscribed to table_changes" }

                        while (isActive) {
                            // Check for notifications received since the last poll
                            val notifications = pgConn.notifications

                            if (notifications != null && notifications.isNotEmpty()) {
                                logger.debug { "Received ${notifications.size} notifications from Postgres" }

                                notifications.forEach { notification ->
                                    val payload = notification.parameter
                                    logger.trace { "Raw Notification Payload: $payload" }

                                    if (payload.trim().startsWith("{")) {
                                        // Parse structured JSON notification payload
                                        try {
                                            val jsonObject = Json.parseToJsonElement(payload).jsonObject
                                            val entityType = jsonObject["entityType"]?.jsonPrimitive?.content ?: "UNKNOWN"
                                            val entityId = jsonObject["entityId"]?.jsonPrimitive?.content ?: ""
                                            val orgId = jsonObject["organizationId"]?.jsonPrimitive?.content ?: ""
                                            val wsId = jsonObject["workspaceId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }
                                            val branchId = jsonObject["branchId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" } ?: wsId
                                            val action = jsonObject["action"]?.jsonPrimitive?.content ?: "SYNC"
                                            val seq = jsonObject["seq"]?.jsonPrimitive?.long ?: 0L

                                            val event = EntityChangeEvent(
                                                seq = seq,
                                                entityType = entityType.uppercase(),
                                                entityId = entityId,
                                                organizationId = orgId,
                                                workspaceId = wsId,
                                                branchId = branchId,
                                                action = action,
                                                timestamp = Clock.System.now()
                                            )

                                            EventBus.publish(event)
                                            logger.debug { "Published JSON EventBus change: ${event.entityType} | ${event.action} | ID: ${event.entityId} | WS: ${event.workspaceId}" }
                                        } catch (e: Exception) {
                                            logger.error(e) { "Failed to parse JSON system event: $payload" }
                                        }
                                    } else {
                                        // Fallback to pipe-delimited parsing
                                        val parts = payload.split("|")

                                        if (parts.size >= 7) {
                                            val normalizedType = parts[1].uppercase()

                                            try {
                                                val wsId = parts[4].takeIf { it.isNotBlank() && it != "null" }
                                                val branchId = parts[5].takeIf { it.isNotBlank() && it != "null" } ?: wsId
                                                val event = EntityChangeEvent(
                                                    seq = parts[0].toLong(),
                                                    entityType = normalizedType,
                                                    entityId = parts[2],
                                                    organizationId = parts[3],
                                                    workspaceId = wsId,
                                                    branchId = branchId,
                                                    action = parts[6], // "INSERT", "UPDATE", "DELETE"
                                                    timestamp = Clock.System.now()
                                                )

                                                EventBus.publish(event)
                                                logger.debug { "Published EventBus change: ${event.entityType} | ${event.action} | ID: ${event.entityId} | WS: ${event.workspaceId}" }
                                            } catch (e: Exception) {
                                                logger.error(e) { "Failed to parse system event: $payload" }
                                            }
                                        } else if (parts.size >= 5) {
                                            val normalizedType = parts[1].uppercase()

                                            try {
                                                val event = EntityChangeEvent(
                                                    seq = parts[0].toLong(),
                                                    entityType = normalizedType,
                                                    entityId = parts[2],
                                                    organizationId = parts[3],
                                                    workspaceId = null,
                                                    branchId = null,
                                                    action = parts[4], // "INSERT", "UPDATE", "DELETE"
                                                    timestamp = Clock.System.now()
                                                )

                                                EventBus.publish(event)
                                                logger.debug { "Published EventBus change: ${event.entityType} | ${event.action} | ID: ${event.entityId}" }
                                            } catch (e: Exception) {
                                                logger.error(e) { "Failed to parse system event: $payload" }
                                            }
                                        } else {
                                            logger.warn { "Malformed notification payload (expected at least 5 parts): $payload" }
                                        }
                                    }
                                }
                            }

                            // Prevent tight loop and poll for notifications
                            delay(500.milliseconds)

                            // Keep-alive: Verify connection is still valid
                            if (conn.isClosed) {
                                throw SQLException("Database connection lost")
                            }

                            // Optional: Clear notifications queue by executing a simple query
                            conn.createStatement().execute("SELECT 1")
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Database Notification Listener Error: ${e.message}. Retrying in 5s..." }
                    delay(5000.milliseconds)
                }
            }
        }
    }
}
