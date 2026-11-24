package com.citacomsys.core.mesh.gateway

import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.auth.HubNodeAuthenticationPlugin
import com.citacomsys.core.mesh.service.MeshManager
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.ServerSentEvent
import kotlinx.serialization.json.Json

/**
 * Centrally configures the Mother Hub's Ktor server logic using pluggable gateway route registration.
 */
internal fun Application.configureHubServer(
    gateways: List<HubServiceGateway>,
    meshManager: MeshManager,
    plugins: List<HubPlugin>
) {
    install(DoubleReceive)
    install(SSE)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    routing {
        // ---  SATELLITE SECURITY GATE ---
        install(HubNodeAuthenticationPlugin) {}

        // --- CORE MESH PROTOCOL (CONTROL PLANE) ---
        route("api/v1/mesh") {
            post("activity") {
                val activity = call.receive<NodeActivity>()
                call.respond(HttpStatusCode.OK)
            }

            // Real-time Mesh Signals (Pub/Sub via SSE)
            sse("signals") {
                meshManager.meshSignals.collect { activity ->
                    send(
                        ServerSentEvent(
                            data = Json.encodeToString(NodeActivity.serializer(), activity),
                            event = "mesh_signal"
                        )
                    )
                }
            }
        }

        // --- PLUGGABLE INDUSTRIAL ROUTES ---
        plugins.forEach { plugin ->
            with(plugin) { register(this@routing) }
        }

        // --- INDUSTRIAL GATEWAYS (DATA PLANE) ---
        route("api/v1") {

            // Fallback / Catch-all handler for unhandled industrial routes
            val fallbackHandler: suspend RoutingContext.() -> Unit = {
                call.respond(HttpStatusCode.Unauthorized)
            }

            get("{...}", fallbackHandler)
            post("{...}", fallbackHandler)
            put("{...}", fallbackHandler)
            delete("{...}", fallbackHandler)
        }
    }
}
