package com.citacomsys.inventory.admin.system

import com.citacomsys.api.platform.model.PlatformDemo
import com.citacomsys.api.platform.model.PlatformFeature
import com.citacomsys.api.platform.model.PlatformTestimonial
import com.citacomsys.api.platform.model.ProductPackageDetails
import com.citacomsys.api.platform.model.PromotionCampaign
import com.citacomsys.api.platform.model.SmsPackage
import com.citacomsys.api.platform.model.SystemSettings
import com.citacomsys.types.PageQuery
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.adminSystemRoute(service: AdminSystemService) {
    route("/system") {

        // --- Product Package Management ---
        route("/packages") {
            post {
                val query = call.receive<PageQuery>()
                val result = service.fetchPackages(query)
                call.respond(result)
            }

            get("/{pkgId}") {
                val id = call.parameters["pkgId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val details = service.getPackageDetails(id)
                call.respond(details)
            }

            post("/{pkgId}") {
                val details = call.receive<ProductPackageDetails>()
                service.savePackage(details).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }
        }

        // --- SMS Bundle Management ---
        route("sms") {
            get {
                val packages = service.fetchSmsPackages()
                call.respond(packages)
            }
            post {
                val pkg = call.receive<SmsPackage>()
                service.saveSmsPackage(pkg).getOrThrow()
                call.respond(HttpStatusCode.Created)
            }
        }

        // --- Promotion Campaign Management ---
        route("/campaigns") {
            get {
                val campaigns = service.fetchCampaigns()
                call.respond(campaigns)
            }
            post {
                val campaign = call.receive<PromotionCampaign>()
                service.saveCampaign(campaign).getOrThrow()
                call.respond(HttpStatusCode.Created)
            }
        }

        // --- Platform Content (CMS) ---
        route("/content") {
            get {
                val content = service.getPlatformContent()
                call.respond(content)
            }
            post("feature") {
                val feature = call.receive<PlatformFeature>()
                service.saveFeature(feature).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }
            post("demo") {
                val demo = call.receive<PlatformDemo>()
                service.saveDemo(demo).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }
            post("testimonial") {
                val testimonial = call.receive<PlatformTestimonial>()
                service.saveTestimonial(testimonial).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }
        }

        // --- Global Platform Settings ---
        route("settings") {
            get {
                val settings = service.getSettings()
                call.respond(settings)
            }
            post {
                val settings = call.receive<SystemSettings>()
                service.updateSettings(settings).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }
        }

        // --- Role & Capability Management ---
        route("/roles") {
            get("/capabilities") {
                val caps = service.fetchGlobalRoleCapabilities()
                call.respond(caps)
            }
            post("/{role}/capabilities") {
                val role = call.parameters["role"]!!
                val capabilities = call.receive<Set<com.citacomsys.types.UserCapability>>()
                service.saveGlobalRoleCapabilities(role, capabilities).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }

            get("/inheritance") {
                val inheritance = service.fetchRoleInheritance()
                call.respond(inheritance)
            }
            post("/inheritance") {
                val req = call.receive<Map<String, String>>()
                service.saveRoleInheritance(req["role"]!!, req["parentRole"]!!).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }
            post("/inheritance/delete") {
                val req = call.receive<Map<String, String>>()
                service.deleteRoleInheritance(req["role"]!!, req["parentRole"]!!).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
