package com.citacomsys.inventory.admin.dashboard

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

internal fun Route.adminDashboardRoute(service: AdminDashboardService) {
    route("/dashboard") {
        get("/summary") {
            val summary = service.getPlatformSummary()
            call.respond(summary)
        }
    }
}
