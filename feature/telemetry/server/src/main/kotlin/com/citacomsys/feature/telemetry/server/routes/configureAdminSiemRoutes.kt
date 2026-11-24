package com.citacomsys.feature.telemetry.server.routes

import com.citacomsys.feature.telemetry.server.repository.SiemTraceRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

internal fun Route.configureAdminSiemRoutes() {
    val traceRepository: SiemTraceRepository by inject()

    route("siem") {

        // Example: GET .../siem/trace/e2b34a68-5872-4b68-b80c
        get("/trace/{requestId}") {
            val targetRequestId = call.parameters["requestId"]

            if (targetRequestId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing required parameter 'requestId'")
                )
                return@get
            }

            val timelineData = traceRepository.getTimelineByRequestId(targetRequestId)

            if (timelineData.isEmpty()) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "No logs recorded for Request ID: $targetRequestId")
                )
                return@get
            }

            call.respond(HttpStatusCode.OK, timelineData)
        }

        // GET /api/v1/admin/siem/events/device/{hardwareId}
        get("/events/device/{hardwareId}") {
            val deviceId = call.parameters["hardwareId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, "Missing hardwareId"
            )

            // Returns a unified list of TraceTimelineEntry (Identity + Network + Endpoint logs)
            val timeline = traceRepository.getTimelineByDeviceId(deviceId)
            call.respond(timeline)
        }

        // NEW: Global Security Feed (for the main SOC Dashboard)
        get("/events/feed") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val feed = traceRepository.getGlobalSecurityFeed(limit)
            call.respond(feed)
        }
    }
}
