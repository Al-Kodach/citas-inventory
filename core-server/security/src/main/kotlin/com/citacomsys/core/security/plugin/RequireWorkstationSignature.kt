package com.citacomsys.core.security.plugin

import com.citacomsys.core.security.crypto.SignatureVerifier
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import mu.KotlinLogging
import kotlin.math.abs
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

class RequireWorkstationSignatureConfig

/**
 * Defense-in-Depth Route-Scoped Plugin:
 * Enforces that even with a valid JWT (like refresh token), the request MUST originate
 * from the exact same physical hardware holding the private key, preventing replay/theft
 * of refresh tokens.
 */
val RequireWorkstationSignature =
    createRouteScopedPlugin(
        "RequireWorkstationSignature",
        ::RequireWorkstationSignatureConfig
    ) {
        onCall { call ->
            val hardwareId = call.request.headers["X-Hardware-Id"]
            val signature = call.request.headers["X-Workstation-Signature"]
            val timestampStr = call.request.headers["X-Timestamp"]
            val workstationPuk = call.request.headers["X-Workstation-Puk"]

            if (hardwareId == null || signature == null || timestampStr == null || workstationPuk == null) {
                logger.warn { "Defense-in-Depth: Missing physical hardware security headers for ${call.request.path()}" }
                call.respond(HttpStatusCode.Unauthorized, "Physical hardware attestation required")
                return@onCall
            }

            // Anti-Replay: Verify timestamp within 5 minutes
            val timestamp = timestampStr.toLongOrNull() ?: 0L
            val now = Clock.System.now().toEpochMilliseconds()
            if (abs(now - timestamp) > 300_000L) {
                logger.warn { "Defense-in-Depth: Workstation clock drift / expired signature for $hardwareId" }
                call.respond(HttpStatusCode.Unauthorized, "Signature expired")
                return@onCall
            }

            // Cryptographic verification supporting both claim formats
            val workspaceId =
                call.request.headers["X-Workspace-Id"] ?: call.request.headers["X-Organization-Id"]
                ?: ""
            val payload1 = "$hardwareId:$workspaceId:$timestamp:"
            val payload2 = "$hardwareId::$timestamp:"

            val isValid = try {
                SignatureVerifier.verify(
                    publicKeyBase64 = workstationPuk,
                    data = payload1.encodeToByteArray(),
                    signatureBase64 = signature
                ) || SignatureVerifier.verify(
                    publicKeyBase64 = workstationPuk,
                    data = payload2.encodeToByteArray(),
                    signatureBase64 = signature
                )
            } catch (e: Exception) {
                logger.error(e) { "Defense-in-Depth: Error verifying workstation signature for $hardwareId" }
                false
            }

            if (!isValid) {
                logger.warn { "Defense-in-Depth: Invalid cryptographic workstation signature for $hardwareId" }
                call.respond(HttpStatusCode.Unauthorized, "Invalid workstation signature")
                return@onCall
            }
        }
    }
