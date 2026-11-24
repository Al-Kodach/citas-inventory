package com.citacomsys.core.security.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import io.ktor.server.response.respond

/**
 * Security plugin to enforce that sensitive API requests originate from the private Tailscale network.
 * 
 * It allows public access to:
 * - /auth/ (For initial login and device binding)
 * - /onboarding/ (For new workspace sign-up)
 */
val TailscaleSecurity = createApplicationPlugin(name = "TailscaleSecurity") {
    onCall { call ->
        val path = call.request.path()
        
        // Allow Public Endpoints
        if (path.startsWith("/auth") || path.startsWith("/onboarding")) {
            return@onCall
        }

        // Check if request is from Tailscale IP range (100.64.0.0/10) or private LAN
        val remoteHost = call.request.origin.remoteHost
        if (!isTailscaleIp(remoteHost)) {
            // Re-verify if we are behind a proxy that might have stripped the Tailscale IP
            val tailscaleHeader = call.request.headers["X-Tailscale-IP"]
            if (tailscaleHeader == null || !isTailscaleIp(tailscaleHeader)) {
                 call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "Security Violation: This endpoint is restricted to the Citas Private Network (Tailscale).")
                )
            }
        }
    }
}

/**
 * Verifies if an IP address belongs to the Tailscale CGNAT range (100.64.0.0/10) or private LAN.
 */
fun isTailscaleIp(ip: String): Boolean {
    val sanitized = ip.trim().lowercase()
    if (sanitized == "127.0.0.1" || 
        sanitized == "::1" || 
        sanitized == "localhost" ||
        sanitized.startsWith("192.168.") ||
        sanitized.startsWith("10.") ||
        sanitized.startsWith("172.") ||
        sanitized.startsWith("0:")
    ) return true // Allow local loopback and private LAN networks
    
    // Tailscale uses the 100.64.0.0/10 range
    if (!sanitized.startsWith("100.")) return false
    
    val parts = sanitized.split(".")
    if (parts.size < 2) return false
    
    val secondOctet = parts[1].toIntOrNull() ?: return false
    return secondOctet in 64..127
}
