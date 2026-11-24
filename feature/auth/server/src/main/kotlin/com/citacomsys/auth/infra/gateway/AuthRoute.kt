package com.citacomsys.auth.infra.gateway

import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.RequireWorkstationSignature
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.authRoute() {
    val controller: AuthController by inject()

    route("/auth") {
        authenticate("device-auth") {
            post("/login") { controller.login(call) }
            post("/verify-otp") { controller.otpChallenge(call) }

            route("/password-resetWorkspaceSession") {
                post("request") { controller.resetPasswordRequest(call) }
                post("submit") { controller.resetPasswordSubmit(call) }
            }
        }

        // ACCESS AUTH: User Sessions (Requires UserPrincipal)
        authenticate("access-auth") {
            install(Authorize)

            post("logout") { controller.logout(call) }
            post("act") { controller.generateAct(call) }

            route("sessions") {
                post("query") { controller.fetchSessions(call) }
            }
        }

        // REFRESH AUTH: Token Refresh
        authenticate("refresh-auth") {
            install(RequireWorkstationSignature)
            post("refresh") {
                controller.refresh(call)
            }
        }
    }
}
