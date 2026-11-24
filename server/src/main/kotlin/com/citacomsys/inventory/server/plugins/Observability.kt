package com.citacomsys.inventory.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.event.Level

fun Application.configureObservability() {

    // 1. Install Call Logging for detailed request logs
    install(CallLogging) {
        level = Level.INFO
        // Customize what you log for each request
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.path()
            "Status: $status, HTTP Method: $httpMethod, Path: $path"
        }
    }

    // 2. Install Micrometer Metrics and expose a /metrics endpoint
//    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
//    install(MicrometerMetrics) {
//        registry = appMicrometerRegistry
//        // Configure which metrics to collect
//        meterBinders = listOf(
//            ClassLoaderMetrics(),
//            JvmMemoryMetrics(),
//            JvmGcMetrics(),
//            ProcessorMetrics(),
//            JvmThreadMetrics(),
//            // LogbackMetrics()
//        )
//    }

    // 3. Create the /metrics route for Prometheus to scrape
    routing {
        get("/metrics") {
//            call.respond(appMicrometerRegistry.scrape())
        }
    }
}