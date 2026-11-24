package com.citacomsys.inventory.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimiting() {
    install(RateLimit) {
        register(RateLimitName("public_tracking")) {
            rateLimiter(limit = 40, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(RateLimitName("auth")) {
            // Strict per-IP rate limiting for login/auth endpoints (15 requests per 60 seconds per IP)
            rateLimiter(limit = 15, refillPeriod = 60.seconds)
            requestKey { call -> call.request.origin.remoteHost }
        }
        global {
            rateLimiter(limit = 1000, refillPeriod = 60.seconds)
            
            // Per-Client Limiting: Use IP to prevent global platform blocks
            requestKey { call -> call.request.origin.remoteHost }

            // Skip heavy or long-lived connections
            requestWeight { call, _ -> 
                if (call.request.uri.contains("/events/subscribe")) 0 else 1 
            }
        }
    }
}
