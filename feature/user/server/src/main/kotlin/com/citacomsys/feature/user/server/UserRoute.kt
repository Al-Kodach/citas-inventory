package com.citacomsys.feature.user.server

import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.types.UserCapability
import com.citacomsys.types.ProductModule
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.userRouting() {
    val controller: UserController by inject()


    route("api/v1/users/self") {
        authenticate("access-auth") {
            install(Authorize) {
                scope = SecurityScope.ORGANIZATION
            }

            authenticate("access-auth", "device-access-auth") {
                get("workspace-bindings") { controller.getSelfWorkspaceBindings(call) }
            }
        }
    }

    route("api/v1/users") {
        install(Authorize) {
            scope = SecurityScope.ORGANIZATION
            module = ProductModule.USER_MANAGEMENT
            requireCapabilities = setOf(UserCapability.MANAGE_USERS)
        }

        authenticate("access-auth") {
            get("/{userId}") { controller.findById(call) }
            get("/phone/{phone}") { controller.findByPhone(call) }
            get("/{userId}/workspace-bindings") { controller.getUserWorkspaceBindingsById(call) }

            route("/{batchId}") {
                put("/suspend") { controller.suspendUser(call) }
                put("/activate") { controller.activateUser(call) }
                delete { controller.delete(call) }
            }

            post { controller.save(call) }
            post("bind-workspace") { controller.bindUserToWorkspace(call) }
            post("unbind-workspace") { controller.unbindUserFromWorkspace(call) }
            get("active-sessions") { controller.getActiveSessions(call) }
            post("/paged") { controller.fetchPaged(call) }
        }

        authenticate("device-access-auth") {
            get("/sync") { controller.changeList(call) }
        }
    }

    route("api/v1/settings/profile") {
        install(Authorize) {
            scope = SecurityScope.ORGANIZATION
        }

        authenticate("access-auth") {
            post("change-password") {
                controller.changePassword(call)
            }
            post("mfa/enable/request") {
                controller.requestMfaEnable(call)
            }
            post("mfa/enable/confirm") {
                controller.confirmMfaEnable(call)
            }
            post("mfa/disable/request") {
                controller.requestMfaDisable(call)
            }
            post("mfa/disable") {
                controller.disableMfa(call)
            }
        }
    }
}
