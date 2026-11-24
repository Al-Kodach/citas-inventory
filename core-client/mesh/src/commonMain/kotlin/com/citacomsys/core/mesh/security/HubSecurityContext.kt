package com.citacomsys.core.mesh.security

import com.citacomsys.types.SyncMetadata
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

val HubSecurityContextKey = AttributeKey<SyncMetadata>("HubSecurityContext")

/**
 * Clean Ktor extension to pull structural mesh tracking information out of any local route.
 */
val ApplicationCall.hubSyncMetadata: SyncMetadata?
    get() = attributes.getOrNull(HubSecurityContextKey)

/**
 * Interceptor applied by Mother Hub Gateways to capture incoming satellite metrics,
 * parse lineage data, and extend the node-hop trace graph.
 */
val InterceptMeshSyncMetadata = createRouteScopedPlugin("InterceptMeshSyncMetadata") {
    val json = Json { ignoreUnknownKeys = true }

    onCall { call ->
        val encodedMeta = call.request.header("X-Sync-Metadata")

        if (!encodedMeta.isNullOrBlank()) {
            try {
                // Decode incoming Base64 JSON payload from satellite node
                val decodedBytes = Base64.decode(encodedMeta)
                val jsonStr = decodedBytes.decodeToString()
                val incomingMeta = json.decodeFromString<SyncMetadata>(jsonStr)

                // Attach metadata to call attributes so gateways can access it cleanly via call.hubSyncMetadata
                call.attributes.put(HubSecurityContextKey, incomingMeta)
            } catch (e: Exception) {
                // If metadata parsing breaks down structurally, reject manipulation safely
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Invalid or corrupted X-Sync-Metadata pipeline headers."
                )
            }
        }
    }
}
