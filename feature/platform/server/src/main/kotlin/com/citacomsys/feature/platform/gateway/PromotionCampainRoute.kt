package com.citacomsys.feature.platform.gateway

import com.citacomsys.api.platform.remote.PromotionCampaignService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Route.promoCampaigns() {
    val service: PromotionCampaignService by inject()

    route("api/v1/campaigns") {
        get {
            val active = service.findActive()
            call.respond(active)
        }

        get("/{batchId}") {
            val id = call.parameters["id"] ?: error("Missing campaign batchId")
            val campaign = service.get(id)
            call.respond(campaign)
        }

        get("code/{code}") {
            val code = call.parameters["code"] ?: error("Missing campaign code")
            val campaign = service.getByCode(code)
            call.respond(campaign)
        }

        post {
            call.respond(Unit)
        }
    }
}