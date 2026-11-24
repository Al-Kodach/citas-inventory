package com.citacomsys.core.persistence

import com.citacomsys.core.persistence.config.DbConfig
import com.citacomsys.shared.event.DeviceRevokedEvent
import com.citacomsys.shared.event.DeviceUpdatedEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.UserSessionRevokedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.postgresql.PGConnection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.text.uppercase
import kotlin.time.Duration.Companion.milliseconds

/**
 * Dedicated PostgreSQL NOTIFY listener for security events.
 * Listens on [security_event_channel] and dispatches strongly-typed [SecurityEvent]s.
 */
internal class PostgresSecurityNotificationListener(
    private val config: DbConfig
) {
    private val logger = KotlinLogging.logger { }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            logger.info { "Starting PostgreSQL Security Notification Listener..." }

            while (isActive) {
                try {
                    DriverManager.getConnection(config.url, config.user, config.pass).use { conn ->
                        val pgConn = conn.unwrap(PGConnection::class.java)

                        conn.createStatement().execute("LISTEN security_event_channel")
                        logger.info { "Successfully subscribed to security_event_channel" }

                        while (isActive) {
                            val notifications = pgConn.notifications

                            if (notifications != null && notifications.isNotEmpty()) {
                                notifications.forEach { notification ->
                                    val payload = notification.parameter.orEmpty().trim()
                                    if (payload.isBlank()) return@forEach

                                    try {
                                        if (payload.startsWith("{")) {
                                            val jsonObject = Json.parseToJsonElement(payload).jsonObject
                                            val entityType = jsonObject["entityType"]?.jsonPrimitive?.content?.uppercase() ?: ""
                                            val entityId = jsonObject["entityId"]?.jsonPrimitive?.content ?: ""
                                            val hardwareId = jsonObject["hardwareId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }
                                            val userId = jsonObject["userId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }
                                            val orgId = jsonObject["organizationId"]?.jsonPrimitive?.content ?: ""
                                            val wsId = jsonObject["workspaceId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }
                                            val action = jsonObject["action"]?.jsonPrimitive?.content ?: "UPDATE"

                                            dispatchSecurityEvent(
                                                jsonObject = jsonObject,
                                                entityType = entityType,
                                                entityId = entityId,
                                                hardwareId = hardwareId,
                                                userId = userId,
                                                orgId = orgId,
                                                wsId = wsId,
                                                action = action
                                            )
                                        }
                                    } catch (e: Exception) {
                                        logger.error(e) { "Failed to process security notification payload: $payload" }
                                    }
                                }
                            }

                            delay(300.milliseconds)

                            if (conn.isClosed) {
                                throw SQLException("Database security listener connection closed")
                            }

                            conn.createStatement().execute("SELECT 1")
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Database Security Notification Listener Error: ${e.message}. Retrying in 5s..." }
                    delay(5000.milliseconds)
                }
            }
        }
    }

    private fun dispatchSecurityEvent(
        jsonObject: JsonObject?,
        entityType: String,
        entityId: String,
        hardwareId: String?,
        userId: String?,
        orgId: String,
        wsId: String?,
        action: String
    ) {
        scope.launch {
            when (entityType) {
                "HARDWARE", "HARDWARE_REGISTRY", "DEVICE" -> {
                    val resolvedHwId = hardwareId ?: entityId
                    val isRevoked = jsonObject?.get("is_revoked")?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                        ?: jsonObject?.get("isRevoked")?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                        ?: false
                    val masterStatus = jsonObject?.get("master_status")?.jsonPrimitive?.content
                        ?: jsonObject?.get("masterStatus")?.jsonPrimitive?.content
                    val isRevokedOrREVOKED = action == "DELETE" || action == "REVOKE" || isRevoked || masterStatus == "REVOKED"

                    if (isRevokedOrREVOKED) {
                        logger.warn { "[SECURITY_NOTIFY] HARDWARE_REVOKED | Action: $action | HW: $resolvedHwId | Status: $masterStatus | Org: $orgId" }
                        EventBus.publish(
                            DeviceRevokedEvent(
                                organizationId = orgId,
                                hardwareId = resolvedHwId,
                                reason = "Hardware locked or revoked ($action)"
                            )
                        )
                    } else {
                        logger.info { "[OPERATION_NOTIFY] HARDWARE_UPDATED | Action: $action | HW: $resolvedHwId | Org: $orgId" }
                        EventBus.publish(
                            DeviceUpdatedEvent(
                                organizationId = orgId,
                                hardwareId = resolvedHwId,
                                workspaceId = wsId
                            )
                        )
                    }
                }

                "USER_SESSIONS", "USER_SESSION", "SESSION" -> {
                    val isRevoked = jsonObject?.get("is_revoked")?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                        ?: jsonObject?.get("isRevoked")?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                        ?: false
                    val isRevokedAction = action == "DELETE" || action == "REVOKE" || isRevoked

                    if (isRevokedAction) {
                        logger.info { "[SECURITY_NOTIFY] USER_SESSION_REVOKED | Action: $action | Session: $entityId | User: $userId | HW: $hardwareId | Org: $orgId" }
                        EventBus.publish(
                            UserSessionRevokedEvent(
                                organizationId = orgId,
                                sessionId = entityId,
                                userId = userId,
                                hardwareId = hardwareId,
                                reason = "User session revoked ($action)"
                            )
                        )
                    }
                }
            }
        }
    }
}
