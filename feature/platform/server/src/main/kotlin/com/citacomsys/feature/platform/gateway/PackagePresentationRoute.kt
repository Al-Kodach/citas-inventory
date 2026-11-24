package com.citacomsys.feature.platform.gateway

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.packagePresentationRoute() {
    val controller: PackagePresentationController by inject()

    authenticate("device-auth") {
        route("api/v1/platform") {
            get("/welcome") {
                controller.getPlatformContent(call)
            }
        }
    }

    authenticate("access-auth") {
        route("api/v1/packages/presentation") {
            get("/public") {
                controller.getPublicPackageUiStates(call)
            }
            get("/sms") {
                controller.getSmsPackageUiStates(call)
            }

            post("/quote") {
                controller.calculateQuote(call)
            }
            post("/invoice/previewInvoice") {
                controller.getInvoicePreview(call)
            }
        }
    }
}
