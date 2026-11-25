package com.citacomsys.core.persistence

import com.citacomsys.core.persistence.config.DbConfig
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.OrgRuntimeConfig
import com.citacomsys.shared.event.ThemeEvent
import com.citacomsys.shared.event.UserWorkspaceBindingEvent
import com.citacomsys.shared.event.WorkspaceEvent
import com.citacomsys.shared.event.WorkstationBindingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.postgresql.PGConnection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.text.uppercase
import kotlin.time.Duration.Companion.milliseconds

/**
 * Dedicated PostgreSQL NOTIFY listener for operational/admin events.
 * Listens on [operation_event_channel] and dispatches strongly-typed [OperationEvent]s for targeted business, user, and org updates.
 */
internal class PostgresOperationNotificationListener(
    private val config: DbConfig
) {
    private val logger = KotlinLogging.logger { }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            logger.info { "Starting PostgreSQL Operation Notification Listener..." }

            while (isActive) {
                try {
                    DriverManager.getConnection(config.url, config.user, config.pass).use { conn ->
                        val pgConn = conn.unwrap(PGConnection::class.java)

                        conn.createStatement().execute("LISTEN operation_event_channel")
                        logger.info { "Successfully subscribed to operation_event_channel" }

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
                                            val orgId = jsonObject["organizationId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" } ?: ""
                                            val wsId = jsonObject["workspaceId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }
                                            val action = jsonObject["action"]?.jsonPrimitive?.content ?: "UPDATE"

                                            dispatchOperationEvent(
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
                                        logger.error(e) { "Failed to process operation notification payload: $payload" }
                                    }
                                }
                            }

                            delay(300.milliseconds)

                            if (conn.isClosed) {
                                throw SQLException("Database operation listener connection closed")
                            }

                            conn.createStatement().execute("SELECT 1")
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Database Operation Notification Listener Error: ${e.message}. Retrying in 5s..." }
                    delay(5000.milliseconds)
                }
            }
        }
    }

    private fun dispatchOperationEvent(
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
                "USER_WORKSPACE_BINDINGS" -> {
                    val targetUserId = userId ?: entityId
                    logger.info { "[OPERATION_NOTIFY] USER_WORKSPACE_BINDINGS | Action: $action | User: $targetUserId | WS: $wsId | Org: $orgId" }
                    if (targetUserId.isNotBlank()) {
                        EventBus.publish(
                            UserWorkspaceBindingEvent(
                                userId = targetUserId,
                                workspaceId = wsId,
                                action = action,
                                organizationId = orgId
                            )
                        )
                    }
                }

                "WORKSTATION_BINDINGS", "BINDING" -> {
                    val targetHwId = hardwareId ?: entityId
                    logger.info { "[OPERATION_NOTIFY] WORKSTATION_BINDINGS | Action: $action | HW: $targetHwId | WS: $wsId | Org: $orgId" }
                    if (targetHwId.isNotBlank()) {
                        EventBus.publish(
                            WorkstationBindingEvent(
                                hardwareId = targetHwId,
                                workspaceId = wsId,
                                action = action,
                                organizationId = orgId
                            )
                        )
                    }
                }

                "WORKSPACE", "WORKSPACES" -> {
                    val resolvedWsId = wsId ?: entityId
                    logger.info { "[OPERATION_NOTIFY] WORKSPACE | Action: $action | WS: $resolvedWsId | Org: $orgId" }
                    if (orgId.isNotBlank()) {
                        EventBus.publish(
                            WorkspaceEvent(
                                workspaceId = resolvedWsId,
                                action = action,
                                organizationId = orgId
                            )
                        )
                    }
                }

                "ORGANIZATION", "ORGANIZATIONS", "TENANT" -> {
                    val resolvedOrgId = orgId.ifBlank { entityId }
                    logger.info { "[OPERATION_NOTIFY] ORGANIZATION | Action: $action | Org: $resolvedOrgId" }
                    if (resolvedOrgId.isNotBlank()) {
                        EventBus.publish(
                            OrgRuntimeConfig(
                                action = action,
                                organizationId = resolvedOrgId
                            )
                        )
                    }
                }

                "BRANDING", "THEME" -> {
                    val targetId = wsId ?: orgId.ifBlank { entityId }
                    val isOrg = wsId.isNullOrBlank()
                    logger.info { "[OPERATION_NOTIFY] BRANDING | Action: $action | Target: $targetId (isOrg: $isOrg)" }
                    EventBus.publish(
                        ThemeEvent(
                            targetId = targetId,
                            isOrg = isOrg,
                            action = action,
                            organizationId = orgId
                        )
                    )
                }
            }
        }
    }
}
