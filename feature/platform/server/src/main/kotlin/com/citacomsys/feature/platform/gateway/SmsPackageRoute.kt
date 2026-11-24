package com.citacomsys.feature.platform.gateway

import com.citacomsys.api.platform.remote.SmsPackageService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.smsPackage() {
    val service: SmsPackageService by inject()

    route("api/v1/sms-packages") {
        get {
            val packages = service.getActive()
            call.respond(packages)
        }

        get("/{batchId}") {
            val id = call.parameters["id"] ?: error("Missing package batchId")
            val pkg = service.get(id)
            call.respond(pkg)
        }

        post {
            call.respond(Unit)
        }
    }
}