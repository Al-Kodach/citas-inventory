package com.citacomsys.inventory.server.plugins

import com.citacomsys.auth.infra.gateway.authRoute
import com.citacomsys.core.domain.result.SystemActors.SYSTEM_USER_ID
import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.core.security.service.GeoFencingService
import com.citacomsys.core.security.service.MfaService
import com.citacomsys.core.storage.configureStaticArtifactsHosting
import com.citacomsys.device.infra.gateway.deviceRoute
import com.citacomsys.feature.platform.gateway.packagePresentationRoute
import com.citacomsys.feature.platform.gateway.packageRoute
import com.citacomsys.feature.platform.gateway.promoCampaigns
import com.citacomsys.feature.platform.gateway.smsPackage
import com.citacomsys.feature.telemetry.server.configureTelemetryRoutes
import com.citacomsys.feature.user.server.userRouting
import com.citacomsys.inventory.home.server.homeRoute
import com.citacomsys.inventory.server.route.sseRouting
import com.citacomsys.module.inventory.gateway.inventory
import com.citacomsys.organization.infra.gateway.organizationOnboardRoute
import com.citacomsys.organization.infra.gateway.organizationRoute
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.HttpRequestLifecycle
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.koin

private val logger = KotlinLogging.logger { }

fun Application.configureRouting() {
    install(XForwardedHeaders)
    install(SSE)
    install(DoubleReceive)
    install(HttpRequestLifecycle) {
        cancelCallOnClose = true
    }

    install(ContentNegotiation) {
        json(this@configureRouting.koin().get<Json>())
    }

    val geoFencingService = GeoFencingService()

    configureGeoFencing(geoFencingService)

    routing {
        get("api/v1/citacomsys") {
            call.respondText("Welcome to CITACOM SYSTEMS LTD")
        }

        post("api/v1/login/primary") {
            val remoteAddress = call.request.origin.remoteAddress
            val host = call.request.origin.remoteHost

            logger.debug { "Login request from $remoteAddress, $host" }

            call.respondText("Welcome to citacomsys")
            return@post
        }

        post("api/v1/login/verify-otp") {
            val service: MfaService by inject()
            val request = call.receive<Map<String, String>>()
            val otpCode = request["otpCode"]

            val result = service.verify(
                userId = SYSTEM_USER_ID.toString(),
                code = otpCode!!,
                purpose = OtpPurpose.MFA_CHALLENGE
            )

            call.respondText(result.toString())
        }

        configureTelemetryRoutes()
        sseRouting()

        // Strict per-IP Rate Limiting for Authentication, Onboarding & Platform Routes
        rateLimit(RateLimitName("auth")) {
            authRoute()
            organizationOnboardRoute()
        }

        userRouting()
        packageRoute()
        smsPackage()
        promoCampaigns()
        deviceRoute()
        homeRoute()
        inventory()
        packagePresentationRoute()
        organizationRoute()
        configureStaticArtifactsHosting("./build-artifacts-vault")
    }
}
