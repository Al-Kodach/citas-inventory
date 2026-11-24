package com.citacomsys.inventory.server.plugins

import com.citacomsys.core.security.service.GeoFencingService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.header
import io.ktor.server.response.*

fun Application.configureGeoFencing(geoFencingService: GeoFencingService) {
    intercept(ApplicationCallPipeline.Plugins) {
        val forwardedFor = call.request.header("X-Forwarded-For")
        val realIp = call.request.header("X-Real-IP")
        val clientIp = forwardedFor?.split(",")?.firstOrNull()?.trim()
            ?: realIp?.trim()
            ?: call.request.origin.remoteHost

        if (!geoFencingService.isTrafficAllowed(clientIp)) {
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf("error" to "Access Denied. This service is only available within Ghana.")
            )
            return@intercept finish()
        }
    }
}
