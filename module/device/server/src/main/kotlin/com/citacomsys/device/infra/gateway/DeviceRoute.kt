package com.citacomsys.device.infra.gateway

import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.types.UserCapability
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.deviceRoute() {
    val controller: DeviceController by inject()

    route("api/v1/devices") {
        authenticate("access-auth") {

            // Block 1: Read-Only Inspection Routes (Organization Scope)
            route("") {
                install(Authorize) {
                    scope = SecurityScope.ORGANIZATION
                }

                // Get all devices registered in organization
                get {
                    controller.getAllDevices(call)
                }

                // Get bindings for active workspace
                get("bindings") {
                    controller.getBindings(call)
                }

                // Get device summary with hardware details and workspace bindings
                get("{id}/summary") {
                    controller.getDeviceSummary(call)
                }

                // Get bindings for a specific hardware ID
                get("{id}/bindings") {
                    controller.getDeviceBindings(call)
                }

                // Get active user sessions for a specific hardware ID
                get("{id}/sessions") {
                    controller.getSessions(call)
                }
            }

            // Block 2: Write / Dangerous Device Management Operations (Requires MANAGE_ORGANIZATIONS Capability)
            route("/admin") {
                install(Authorize) {
                    scope = SecurityScope.ORGANIZATION
                    requireCapabilities = setOf(UserCapability.MANAGE_ORGANIZATIONS)
                }

                // Rename device
                put("{id}/rename") {
                    controller.renameDevice(call)
                }

                // Revoke device globally
                post("{id}/revoke") {
                    controller.revokeDeviceGlobally(call)
                }

                // Unbind a specific workstation binding
                delete("bindings/{bindingId}") {
                    controller.unbindWorkstation(call)
                }

                // Relink a specific workstation binding
                post("bindings/{bindingId}/relink") {
                    controller.relinkWorkstation(call)
                }

                // Revoke active user session
                post("sessions/{sessionId}/revoke") {
                    controller.revokeSession(call)
                }
            }
        }
    }
}
