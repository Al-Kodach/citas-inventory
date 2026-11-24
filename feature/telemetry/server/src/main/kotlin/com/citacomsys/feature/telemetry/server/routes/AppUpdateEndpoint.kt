package com.citacomsys.feature.telemetry.server.routes

import com.citacomsys.core.security.data.AppReleaseRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.appUpdateRoute(
    releaseRepository: AppReleaseRepository
) {
    route("/api/v1/telemetry/app-updates") {
        get("/latest") {
            val flavor = call.request.header("X-App-Flavor") ?: "unknown"
            val activeReleaseResponse = releaseRepository.getLatestApplicableRelease(flavor)

            if (activeReleaseResponse != null) {
                call.respond(HttpStatusCode.OK, activeReleaseResponse)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
