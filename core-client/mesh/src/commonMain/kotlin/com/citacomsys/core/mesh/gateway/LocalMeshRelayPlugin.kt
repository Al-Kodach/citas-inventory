package com.citacomsys.core.mesh.gateway

import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.mesh.security.MeshProtocol
import com.citacomsys.core.domain.error.LanOnlyWorkspaceHubUnavailableException
import com.citacomsys.types.NodeMeshMode
import io.github.aakira.napier.Napier
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.network.*

val MeshRetryCountKey = AttributeKey<Int>("MeshRetryCount")

class LocalMeshRelayConfig {
    var preferences: SyncPreferencesRepository? = null
}


/**
 * Intercepts cloud requests and redirects them to the local Mother Hub if discovered.
 * Optimizes bandwidth by keeping industrial traffic on the LAN protected by SSL/TLS.
 */

val OriginalCloudUrlKey = AttributeKey<Url>("OriginalCloudUrlKey")

val TargetWorkspaceIdKey = AttributeKey<String>("TargetWorkspaceIdKey")

val LocalMeshRelayPlugin = createClientPlugin("LocalMeshRelayPlugin", ::LocalMeshRelayConfig) {
    val prefs = pluginConfig.preferences

    onRequest { request, _ ->
        prefs ?: return@onRequest
        if (prefs.isMotherHubEnabled.value) return@onRequest

        if (!request.attributes.contains(OriginalCloudUrlKey)) {
            request.attributes.put(OriginalCloudUrlKey, request.url.build())
        }

        val wsId = request.attributes.getOrNull(TargetWorkspaceIdKey)
            ?: request.headers["X-Workspace-Id"]

        val isCloudAllowed = wsId == null || prefs.syncConfig.value.targets.any {
            it.workspaceId == wsId && it.isCloudDeployed
        }

        val activeHubIp = prefs.activeHubIp.value
        val mode = prefs.meshSyncMode.value

        if (activeHubIp != null && mode != NodeMeshMode.NODE) {
            request.url {
                protocol = URLProtocol.HTTPS
                host = activeHubIp
                port = prefs.activeHubPort.value
            }
            request.header("X-Mesh-Source", "SATELLITE")
        } else {
            // Block cloud fallback immediately on the client if workspace is LAN-only
            if (!isCloudAllowed) {
                throw LanOnlyWorkspaceHubUnavailableException(
                    "Workspace [$wsId] is LAN-only (isCloudDeployed=false) and Mother Hub is unreachable."
                )
            }

            request.attributes.getOrNull(OriginalCloudUrlKey)?.let { cloudUrl ->
                request.url.takeFrom(cloudUrl)
            }
            request.headers.remove("X-Mesh-Source")
        }
    }
}

/**
 * Validator that catches LAN connection failures and triggers re-discovery or fail-fast.
 */
fun HttpRequestRetryConfig.installMeshRetryPolicy(
    prefs: SyncPreferencesRepository
) {
    maxRetries = 4 // Up to 3 LAN attempts + 1 cloud failover attempt

    retryOnExceptionIf(maxRetries = 4) { request, cause ->
        // Never retry if blocked by local LAN-only workspace policy
        if (cause is LanOnlyWorkspaceHubUnavailableException) return@retryOnExceptionIf false

        if (prefs.isMotherHubEnabled.value || prefs.meshSyncMode.value == NodeMeshMode.NODE) {
            return@retryOnExceptionIf false
        }

        val isHubError = cause.message?.contains("refused") == true ||
                cause is UnresolvedAddressException ||
                cause is ConnectTimeoutException ||
                cause is SocketTimeoutException

        if (!isHubError || prefs.activeHubIp.value == null) return@retryOnExceptionIf false

        val currentRetry = request.attributes.getOrNull(MeshRetryCountKey) ?: 0
        val mode = prefs.meshSyncMode.value

        if (currentRetry < 3) {
            val nextRetry = currentRetry + 1
            request.attributes.put(MeshRetryCountKey, nextRetry)
            Napier.d { "LocalMeshRelay: Retrying LAN Hub (Attempt $nextRetry/3)" }
            return@retryOnExceptionIf true
        }

        // All 3 LAN attempts failed -> clear stale Hub IP to trigger LAN re-discovery
        prefs.setActiveHubInfo(null, MeshProtocol.TLS_PORT, null, null, null)
        request.attributes.put(MeshRetryCountKey, 0)

        // Only allow 4th attempt (Cloud failover) if in HYBRID mode AND workspace is cloud-deployed
        val wsId = request.attributes.getOrNull(TargetWorkspaceIdKey) ?: request.headers["X-Workspace-Id"]
        val isCloudAllowed = wsId == null || prefs.syncConfig.value.targets.any {
            it.workspaceId == wsId && it.isCloudDeployed
        }

        if (mode == NodeMeshMode.HYBRID && isCloudAllowed) {
            Napier.w { "LocalMeshRelay: LAN Hub unreachable after 3 attempts. Failing over to Cloud VPS." }
            true
        } else {
            Napier.w { "LocalMeshRelay: LAN Hub unreachable and Cloud failover disallowed (mode=$mode, cloudAllowed=$isCloudAllowed)." }
            false
        }
    }

    delayMillis { retry ->
        if (prefs.meshSyncMode.value == NodeMeshMode.SATELLITE) {
            (retry * 2000L).coerceAtMost(30_000L)
        } else {
            1000L
        }
    }
}

