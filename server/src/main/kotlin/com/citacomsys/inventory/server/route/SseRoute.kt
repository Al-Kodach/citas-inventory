package com.citacomsys.inventory.server.route

import com.citacomsys.core.notification.service.SseService
import com.citacomsys.core.security.plugin.resolveHardwareId
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.feature.api.telemetry.SystemEventService
import com.citacomsys.shared.event.ClientType
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.event.SseClient
import com.citacomsys.shared.event.SseSubscriptionRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Route.sseRouting() {
    val sseService: SseService by inject()
    val eventService: SystemEventService<EntityChangeEvent> by inject()

    authenticate("device-access-auth") {
        sse("api/v1/citacomsys/inventory/events/subscribe") {
            val deviceId = call.resolveHardwareId()
            val organizationId = call.resolveOrganizationId
            val workspaceId = runCatching { call.resolvedWorkspaceId }.getOrNull()
            val branchId = call.request.queryParameters["branchId"]?.takeIf { it.isNotBlank() }

            val clientType = call.request.queryParameters["clientType"]?.let {
                runCatching { ClientType.valueOf(it) }.getOrNull()
            } ?: ClientType.POS_TERMINAL

            val lastSeq = call.request.queryParameters["lastSeq"]?.toLongOrNull() ?: 0L

            if (lastSeq > 0 && !workspaceId.isNullOrBlank()) {
                val missedEvents = eventService.findAfter(orgId = organizationId, lastSeq, workspaceId)
                missedEvents.forEach { event ->
                    send(
                        ServerSentEvent(
                            event = EntityChangeEvent.name,
                            data = Json.encodeToString(event),
                            id = event.seq.toString()
                        )
                    )
                }
            }

            val topics = call.request.queryParameters.getAll("topics")
                ?.flatMap { it.split(',') }
                ?.filter { it.isNotBlank() }
                ?.toSet() ?: setOf("INVENTORY_STOCK", "SUBSCRIPTION", "BILLING")

            val client = SseClient(
                id = deviceId,
                organizationId = organizationId,
                workspaceId = workspaceId,
                branchId = branchId,
                topics = topics,
                clientType = clientType
            )

            sseService.addClient(client, this)

            try {
                awaitCancellation()
            } finally {
                sseService.removeClient(deviceId)
            }
        }

        post("api/v1/citacomsys/inventory/events/update-subscription") {
            val deviceId = call.resolveHardwareId()
            val organizationId = call.resolveOrganizationId
            require(deviceId.isNotBlank()) { "Missing or invalid hardware ID header" }
            val requestBody = call.receive<SseSubscriptionRequest>()
            val secureCommand = requestBody.copy(
                clientId = deviceId,
                organizationId = organizationId
            )
            sseService.subscribe(secureCommand)
            call.respond(HttpStatusCode.OK)
        }
    }
}
