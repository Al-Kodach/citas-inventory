package com.citacomsys.core.remote

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey

val PublicEndpointKey = AttributeKey<Boolean>("PublicEndpointKey")

/**
 * Explicitly marks an HTTP request as a public endpoint, bypassing access token / ACT requirements.
 */
fun HttpRequestBuilder.markAsPublic() {
    attributes.put(PublicEndpointKey, true)
}

object SecurityRegistry {
    // SENSITIVE: Requires Access Token + Action Confirmation Token (ACT)
    private val actionRequiredPaths = setOf(
        "billing/cancel",
        "submit-payment",
        "renewal",
        "sms-topup",
        "settings/security",
        "users/delete",
    )

    // Namespace prefixes — ANY endpoint starting with these prefixes is automatically public
    private val publicPrefixes = setOf(
        "auth/",               // Matches /auth/login, /auth/verify-otp, /auth/password-resetWorkspaceSession/*, etc.
        "admin/auth/",         // Matches all admin authentication routes
        "onboarding/",         // Matches all onboarding routes
        "recovery/",           // Matches all recovery routes
        "telemetry/",          // Matches telemetry/app-updates, etc.
        "api/v1/organization/register",
        "api/v1/organization/verify",
        "api/v1/workspace/recovery",
        "api/v1/platform/welcome",
        "citacomsys/inventory/platform/content",
    )

    fun isPublicRoute(path: String): Boolean {
        val normalized = path.removePrefix("/")
        return publicPrefixes.any { normalized.startsWith(it) }
    }

    fun isActionRequiredRoute(path: String): Boolean {
        val normalized = path.removePrefix("/")
        return actionRequiredPaths.any { normalized.startsWith(it) }
    }
}
