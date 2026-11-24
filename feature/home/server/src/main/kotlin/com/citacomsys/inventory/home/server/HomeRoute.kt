package com.citacomsys.inventory.home.server

import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Main dashboard and home metrics routes.
 */
fun Route.homeRoute() {
    val service: DashboardService by inject()

    authenticate("access-auth") {
        route("/home") {
            get("/dashboard") {
                val branchId = call.request.queryParameters["branchId"]
                val startDate = call.request.queryParameters["startedAt"]
                val endDate = call.request.queryParameters["endAt"]

                val overview = call.transactionManager.runReadOnly {
                    service.getDashboardOverview(
                        workspaceId = call.resolvedWorkspaceId,
                        branchId = branchId,
                        startDate = startDate,
                        endDate = endDate
                    )
                }

                call.application.log.info("Dashboard overview: $overview")
                call.respond(overview)
            }
        }
    }
}
