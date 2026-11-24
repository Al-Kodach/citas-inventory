package com.citacomsys.core.security.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

@Serializable
data class TailscaleKeyRequest(
    val expirySeconds: Int = 600,
    val reusable: Boolean = false,
    val ephemeral: Boolean = false,
    val preauthorized: Boolean = true,
    val tags: List<String> = listOf("tag:client")
)

@Serializable
data class TailscaleKeyResponse(
    val id: String,
    val key: String,
    val expires: String
)

/**
 * Service to interact with the Tailscale API for generating one-time auth keys.
 */
class TailscaleService {
    private val apiKey: String = System.getenv("TAILSCALE_API_KEY") ?: ""
    private val tailnet: String = System.getenv("TAILSCALE_TAILNET") ?: ""

    private val logger = KotlinLogging.logger("TAILSCALE_SERVICE")

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Generates a one-time, pre-authorized Tailscale Auth Key for a new device.
     */
    suspend fun generateAuthKey(): String? {
        if (apiKey.isBlank() || tailnet.isBlank()) {
            logger.warn { "Tailscale API credentials not configured. Key generation skipped." }
            return null
        }

        return try {
            val response: TailscaleKeyResponse = client.post("https://api.tailscale.com/api/v2/tailnet/$tailnet/keys") {
                basicAuth(apiKey, "")
                contentType(ContentType.Application.Json)
                setBody(TailscaleKeyRequest())
            }.body()

            logger.info { "Successfully generated Tailscale auth key (ID: ${response.id})" }
            response.key
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate Tailscale auth key from API" }
            null
        }
    }
}
