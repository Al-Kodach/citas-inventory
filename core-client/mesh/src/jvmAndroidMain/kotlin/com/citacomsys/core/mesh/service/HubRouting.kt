package com.citacomsys.core.mesh.service

import com.citacomsys.core.mesh.auth.HubNodeAuthenticationPlugin
import com.citacomsys.core.mesh.gateway.HubPlugin
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.gateway.MotherHubGateway
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.security.InterceptMeshSyncMetadata
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

internal fun Application.configureHubServer(
    gateways: List<HubServiceGateway>,
    meshSignals: Flow<NodeActivity>,
    broadcastAlert: (NodeActivity) -> Unit,
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

    val motherHubGateway = MotherHubGateway { meshSignals }

    routing {
        install(HubNodeAuthenticationPlugin) {}
        install(InterceptMeshSyncMetadata) {}

        plugins.forEach { plugin ->
            with(plugin) { register(this@routing) }
        }

        route("api/v1") {
            with(motherHubGateway) { registerRoutes(broadcastAlert) }
            gateways.forEach { gateway ->
                with(gateway) { registerRoutes(broadcastAlert) }
            }
        }
    }
}
