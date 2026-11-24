package com.citacomsys.feature.telemetry.server.routes

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.citacomsys.feature.telemetry.server.repository.SiemTraceRepository
import org.koin.ktor.ext.inject
import kotlin.getValue

internal fun Route.configureSiemAnalyticsRoutes() {
    val traceRepository: SiemTraceRepository by inject()
    route("siem/analytics") {

        // GET /api/v1/admin/siem/analytics/threats
        get("/threats") {
            val threats = traceRepository.getTopSecurityThreats(limit = 10)
            call.respond(HttpStatusCode.OK, threats)
        }

        // GET /api/v1/admin/siem/analytics/failures
        get("/failures") {
            val componentFailures = traceRepository.getCommonOperationalFailures(limit = 10)
            call.respond(HttpStatusCode.OK, componentFailures)
        }
    }
}
