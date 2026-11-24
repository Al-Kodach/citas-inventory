package com.citacomsys.core.mesh.gateway

import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.model.SatelliteStatus
import com.citacomsys.types.EntityType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Core Mother Hub Gateway responsible for registering mesh control-plane routes
 * (satellite activity reporting, node registration, and SSE real-time sync signals).
 */
class MotherHubGateway(
    private val meshSignalsProvider: () -> Flow<NodeActivity>
) : HubServiceGateway {
    override val domain: String = "mesh"
    override val entityType: EntityType? = null

    override fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit) {
        route("mesh") {
            post("activity") {
                val activity = call.receive<NodeActivity>()
                broadcastAlert(activity)
                call.respond(HttpStatusCode.OK)
            }

            // Node Registration Endpoint for Satellite Nodes (accepts MeshPeerInfo or SatelliteStatus)
            post("nodes/register") {
                val peerInfo = runCatching {
                    call.receive<SatelliteStatus>().peerInfo
                }.getOrElse {
                    call.receive<MeshPeerInfo>()
                }

                val activity = NodeActivity(
                    bindingId = peerInfo.hardwareId,
                    timestamp = Clock.System.now(),
                    action = "NODE_JOINED",
                    summary = "Satellite Workstation '${peerInfo.workstationName}' registered with Mother Hub",
                    metadata = mapOf(
                        "workstationName" to peerInfo.workstationName,
                        "hardwareId" to peerInfo.hardwareId,
                        "ipAddress" to peerInfo.ipAddress,
                        "organizationId" to peerInfo.organizationId,
                        "workspaceId" to (peerInfo.workspaceId ?: "")
                    )
                )
                broadcastAlert(activity)
                call.respond(HttpStatusCode.OK, mapOf("status" to "registered", "hardwareId" to peerInfo.hardwareId))
            }

            // Real-time Mesh Signals (Pub/Sub via SSE)
            sse("signals") {
                meshSignalsProvider().collect { activity ->
                    send(
                        ServerSentEvent(
                            data = Json.encodeToString(NodeActivity.serializer(), activity),
                            event = "mesh_signal"
                        )
                    )
                }
            }
        }
    }
}
