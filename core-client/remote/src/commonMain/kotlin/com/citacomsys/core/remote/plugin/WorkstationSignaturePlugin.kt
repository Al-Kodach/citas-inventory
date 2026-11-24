package com.citacomsys.core.remote.plugin

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.mesh.security.MeshCryptoManager
import com.citacomsys.core.remote.PublicEndpointKey
import com.citacomsys.core.remote.SecurityRegistry
import com.citacomsys.core.security.crypto.SignatureProvider
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.device.service.DeviceService
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.encodedPath
import kotlin.time.Clock

class WorkstationSignatureConfig {
    lateinit var signatureProvider: SignatureProvider
    lateinit var deviceService: DeviceService
    lateinit var sessionManager: AppSessionManager
    var platformStore: PlatformRuntimeStore? = null
}

private var hasSentHardwareMetadata = false


/**
 * A Ktor Client plugin that automatically attaches asymmetric workstation signatures.
 * Fulfills the "Tokenless Auth" security requirement with versioned claims.
 * Supports multi-workspace operations on a single hardware terminal.
 * Attaches full hardware metadata headers once on first contact to minimize wire overhead.
 */
val WorkstationSignaturePlugin = createClientPlugin(
    "WorkstationSignaturePlugin",
    ::WorkstationSignatureConfig
) {
    val sig = pluginConfig.signatureProvider
    val device = pluginConfig.deviceService
    val session = pluginConfig.sessionManager
    val store = pluginConfig.platformStore

    onRequest { request, _ ->
        val path = request.url.encodedPath
        val isPublic = (request.attributes.getOrNull(PublicEndpointKey) == true) || SecurityRegistry.isPublicRoute(path)

        val hardwareId = device.deviceInfo.value.hardwareId

        val requestWorkspaceId = request.headers["X-Workspace-Id"]
        val workspaceId = requestWorkspaceId?.takeIf { it.isNotBlank() }
            ?: session.activeWorkspaceId ?: ""

        val storeOrgConfig = store?.organizationConfig?.value
        val storeOrgId = storeOrgConfig?.organizationId?.takeIf { it.isNotBlank() }
        val sessionOrgId = session.organizationId?.takeIf { it.isNotBlank() }

        val orgId = if (storeOrgConfig != null) {
            storeOrgId ?: sessionOrgId ?: ""
        } else if (!isPublic) {
            sessionOrgId ?: ""
        } else {
            ""
        }

        val fleetDna = storeOrgConfig?.fleetDna ?: ""
        val timestamp = Clock.System.now().toEpochMilliseconds()

        if (hardwareId.isNotEmpty()) {
            val signature = sig.signWorkstationClaim(
                hardwareId = hardwareId,
                workspaceId = workspaceId,
                timestamp = timestamp,
                nonce = ""
            )

            request.header("X-Hardware-Id", hardwareId)
            request.header("X-Sig-Version", "1")
            request.header("X-Workstation-Puk", sig.getWorkstationPublicKey())

            // Attach full hardware metadata headers once on first contact
            if (!hasSentHardwareMetadata) {
                val hwInfo = device.getHardwareInfo()
                hwInfo.fingerprint?.takeIf { it.isNotBlank() }
                    ?.let { request.header("X-Hardware-Fingerprint", it) }
                hwInfo.model?.takeIf { it.isNotBlank() }
                    ?.let { request.header("X-Hardware-Model", it) }
                hwInfo.osName?.takeIf { it.isNotBlank() }
                    ?.let { request.header("X-Hardware-OS", it) }
                hwInfo.architecture?.takeIf { it.isNotBlank() }
                    ?.let { request.header("X-Hardware-Arch", it) }
                hwInfo.displayName?.takeIf { it.isNotBlank() }
                    ?.let { request.header("X-Hardware-Name", it) }
                hasSentHardwareMetadata = true
            }

            if (workspaceId.isNotEmpty() && !request.headers.contains("X-Workspace-Id")) {
                request.header("X-Workspace-Id", workspaceId)
            }

            if (orgId.isNotEmpty()) {
                request.header("X-Organization-Id", orgId)
            }

            // Satellite / Mesh Deployment Check: Inject active operator/actor info headers
            val isSatellite = workspaceId.isNotEmpty() && !session.isCloudDeployed(workspaceId)
            if (isSatellite) {
                session.currentUserId?.takeIf { it.isNotBlank() }
                    ?.let { request.header("X-Actor-Id", it) }
                session.actorName?.takeIf { it.isNotBlank() }
                    ?.let { request.header("X-Actor-Name", it) }
            }

            // FleetDNA Auth (Organizational Proof)
            val effectiveFleetDna = fleetDna.ifBlank { orgId }
            if (orgId.isNotEmpty() && effectiveFleetDna.isNotEmpty()) {
                val fleetToken =
                    MeshCryptoManager.generateDiscoveryToken(orgId, effectiveFleetDna.encodeToByteArray())
                request.header("X-Fleet-Auth", fleetToken)
            }

            request.header("X-Workstation-Signature", signature)
            request.header("X-Timestamp", timestamp.toString())
        }
    }
}
