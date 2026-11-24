package com.citacomsys.inventory.admin.device

import com.citacomsys.core.persistence.plugin.readTransaction
import com.citacomsys.types.PageQuery
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.deviceRoute(service: AdminDeviceService) {
    route("/workstations") {
        authenticate("admin-access-auth") {
            post {
                val query = call.receive<PageQuery>()
                val devices = call.readTransaction { service.fetch(query) }
                call.respond(devices)
            }
            get("{bindId}") {
                val device = service.getDevice(call.parameters["bindId"]!!)
                call.respond(device)
            }
        }

        authenticate("action-auth") {
            route("{hardwareId}") {
                post("revoke") {
                    val hardwareId = call.parameters["hardwareId"] ?: error("HardwareId Id param missing")
                    val orgID = call.parameters["orId"] ?: error("Organization Id param missing")

//                    val result = service.lockHardwareGlobally().getOrThrow()
//                    call.respond(result)
                }
            }
        }
    }
}