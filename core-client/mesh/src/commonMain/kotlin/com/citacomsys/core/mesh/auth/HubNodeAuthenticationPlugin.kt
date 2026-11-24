package com.citacomsys.core.mesh.auth

import com.citacomsys.core.database.dao.inventory.WorkstationDao
import com.citacomsys.core.mesh.security.MeshCryptoManager
import com.citacomsys.core.mesh.security.createWorkstationSignatureVerifier
import com.citacomsys.kernel.service.PlatformRuntimeStore
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.github.aakira.napier.Napier
import org.koin.ktor.ext.getKoin
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class HubNodeAuthenticationConfig

/**
 * SECURITY COMPLIANCE MODULE: Workstation Identity Verification (Server-Side)
 * This plugin runs on the Mother Hub to verify satellite signatures before processing requests.
 * It enforces that only authorized physical hardware can participate in the local data plane.
 */
@OptIn(ExperimentalTime::class)
val HubNodeAuthenticationPlugin =
    createRouteScopedPlugin("HubNodeAuthenticationPlugin", ::HubNodeAuthenticationConfig) {

        val koin = application.getKoin()
        val workstationDao: WorkstationDao = koin.get()
        val verifier = createWorkstationSignatureVerifier()

        onCall { call ->
            val path = call.request.path()
            
            // PROTOCOL BYPASS: Allow discovery and signaling without full attestation
            if (path.endsWith("/mesh/signals") || path.endsWith("/mesh/activity")) return@onCall

            val hardwareId = call.request.headers["X-Hardware-Id"]
            val hardwareFingerprint = call.request.headers["X-Hardware-Fingerprint"]
            val workspaceId = call.request.headers["X-Workspace-Id"]
            val signature = call.request.headers["X-Workstation-Signature"]
            val timestampStr = call.request.headers["X-Timestamp"]
            val publicKeyFromHeader = call.request.headers["X-Workstation-Puk"]
            val fleetAuthToken = call.request.headers["X-Fleet-Auth"]

            if (hardwareId == null || signature == null || timestampStr == null) {
                Napier.w(tag = "HUB_AUTH") { "Unauthorized: Missing hardware security anchors for $path" }
                call.respond(HttpStatusCode.Unauthorized, "Request must originate from a trusted Citas workstation")
                return@onCall
            }

            // Anti-Replay Protection (5-minute window)
            val timestamp = timestampStr.toLongOrNull() ?: 0L
            val now = Clock.System.now().toEpochMilliseconds()
            if (abs(now - timestamp) > 300_000L) {
                Napier.w(tag = "HUB_AUTH") { "Unauthorized: Security window expired for $hardwareId" }
                call.respond(HttpStatusCode.Unauthorized, "Workstation clock out of sync")
                return@onCall
            }

            // Resolve Identity Anchor (Public Key)
            val registeredWorkstation = workstationDao.findById(hardwareId)
            val publicKey = registeredWorkstation?.public_key ?: publicKeyFromHeader
            
            if (publicKey == null) {
                Napier.w(tag = "HUB_AUTH") { "Unauthorized: Unrecognized hardware identity $hardwareId" }
                call.respond(HttpStatusCode.Unauthorized, "Unregistered workstation")
                return@onCall
            }

            // Hardware Fingerprint Lock
            // Protects against database/key cloning onto different physical hardware
            if (hardwareFingerprint != null && registeredWorkstation?.fingerprint != null) {
                 if (hardwareFingerprint != registeredWorkstation.fingerprint) {
                     Napier.e(tag = "HUB_AUTH") { "SECURITY VIOLATION: Hardware fingerprint mismatch for $hardwareId! Hub Registry=${registeredWorkstation.fingerprint}, Incoming=$hardwareFingerprint" }
                     call.respond(HttpStatusCode.Forbidden, "Hardware identity mismatch detected. Node locked.")
                     return@onCall
                 }
            }

            // Cryptographic Hardware Attestation
            val payload = "$hardwareId:${workspaceId ?: ""}:$timestamp:"
            val isHardwareValid = try {
                verifier.verify(
                    publicKeyBase64 = publicKey,
                    data = payload.encodeToByteArray(),
                    signatureBase64 = signature
                )
            } catch (e: Exception) {
                false
            }

            if (!isHardwareValid) {
                Napier.w(tag = "HUB_AUTH") { "Unauthorized: Cryptographic signature mismatch from $hardwareId" }
                call.respond(HttpStatusCode.Unauthorized, "Invalid workstation signature")
                return@onCall
            }

            // 5. TIERED AUTHORIZATION CHECK
            val isAuthRequest = path.contains("/auth/")
            
            if (!isAuthRequest) {
                // INDUSTRIAL DATA: Requires full FleetDNA proof
                val store = koin.get<PlatformRuntimeStore>()
                val orgId = store.organizationId ?: ""
                val fleetDna = store.organizationConfig.value?.fleetDna ?: ""

                val isFleetValid = fleetAuthToken?.let { token ->
                    MeshCryptoManager.verifyDiscoveryToken(token, orgId, fleetDna.encodeToByteArray())
                } ?: false

                if (!isFleetValid) {
                    Napier.w(tag = "HUB_AUTH") { "Unauthorized: Industrial trust (FleetDNA) missing for $hardwareId" }
                    call.respond(HttpStatusCode.Unauthorized, "Organizational trust not established")
                    return@onCall
                }

                if (registeredWorkstation != null && registeredWorkstation.status != "ACTIVE") {
                    call.respond(HttpStatusCode.Forbidden, "Workstation access ${registeredWorkstation.status}")
                    return@onCall
                }
            } else {
                // AUTH REQUEST: We allow new devices to login via MFA even if not registered yet.
                // We rely on the Cloud to perform MFA and the Hub to record the PUK upon successful relay.
                Napier.i(tag = "HUB_AUTH") { "Permitting auth-gate request from potentially new device $hardwareId" }
            }

            Napier.v(tag = "HUB_AUTH") { "Trust Verified: Request permitted for $path [$hardwareId]" }
        }
    }
