package com.citacomsys.inventory.admin.auth

import com.citacomsys.core.security.plugin.RequireWorkstationSignature
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminAuthRoute() {
    val controller: AdminAuthController by inject()

    route("admin/auth") {
        authenticate("device-login-auth") {
            post("login") { controller.login(call) }
        }

        post("bind-device") { controller.bindDevice(call) }
        post("verify-mfa") { controller.verifyMfa(call) }

        route("device/migration") {
            post("request") { controller.requestMigration(call) }
            post("confirm") { controller.confirmMigration(call) }
        }

        authenticate("admin-access-auth") {
            post("logout") { controller.logout(call) }
            post("act") { controller.generateAct(call) }

            route("password") {
                post("resetWorkspaceSession") { controller.resetPasswordRequest(call) }
                post("submit") { controller.resetPasswordSubmit(call) }
            }
        }

        authenticate("refresh-auth") {
            install(RequireWorkstationSignature)
            post("refresh") {
                controller.refresh(call)
            }
        }
    }
}
