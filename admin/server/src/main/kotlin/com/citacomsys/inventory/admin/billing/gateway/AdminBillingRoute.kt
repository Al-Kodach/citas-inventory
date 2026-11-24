package com.citacomsys.inventory.admin.billing.gateway

import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.core.security.withActionConfirmation
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.ActionConfirmationPurpose
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.types.EntityType
import com.citacomsys.types.UserCapability
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.Json

internal fun Route.adminBillingRoute(
    telemetryAuditor: TelemetryAuditor,
    alertService: SecurityAlertService,
    controller: AdminBillingController,
) {
    route("billing") {
        install(Authorize) {
            scope = SecurityScope.ORGANIZATION
            requireCapabilities = setOf(UserCapability.MANAGE_BILLING)
        }

        authenticate("admin-access-auth") {
            get { controller.fetch(call) }
            get("{batchId}") { controller.getRecord(call) }

            get("/events") {
                call.respondSse(
                    EventBus.subscribe<EntityChangeEvent>()
                        .filter { it.entityType == EntityType.BILLING.name })
            }
        }

        authenticate("action-auth") {
            route("{batchId}") {
                post("reject") {
                    withActionConfirmation(
                        auditor = telemetryAuditor,
                        alertService = alertService,
                        purpose = ActionConfirmationPurpose.REJECT_PAYMENT
                    ) {
                        controller.reject(call)
                    }
                }
            }

            route("{ref}") {
                post("verified") {
                    withActionConfirmation(
                        auditor = telemetryAuditor,
                        alertService = alertService,
                        purpose = ActionConfirmationPurpose.VERIFY_PAYMENT
                    ) {
                        controller.verified(call)
                    }
                }
            }
        }
    }
}

suspend fun ApplicationCall.respondSse(events: Flow<EntityChangeEvent>) {
    response.cacheControl(CacheControl.NoCache(null))
    respondBytesWriter(contentType = ContentType.Text.EventStream) {
        events.collect { event ->
            writeStringUtf8("data: ${Json.encodeToString(event)}\n\n")
            flush()
        }
    }
}