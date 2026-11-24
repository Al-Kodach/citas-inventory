package com.citacomsys.core.security.plugin

import com.citacomsys.core.security.AuthorizationContextProvider
import com.citacomsys.core.security.crypto.SignatureVerifier
import com.citacomsys.core.security.token.jwt.WorkstationDevicePrincipal
import com.citacomsys.core.security.toAuditRequestContext
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.logging.logNetworkSecurityEvent
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.table.tenant.Organizations
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.WorkstationSecurityContext
import io.ktor.server.request.path
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.*
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import com.citacomsys.core.domain.result.toUuid
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import kotlin.math.abs
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

data class HardwareAttestationInfo(
    val isRecognized: Boolean,
    val hasFingerprintMismatch: Boolean,
    val masterStatus: HardwareMasterStatus?,
    val registeredFingerprint: String?,
    val incomingFingerprint: String?
)

val HardwareAttestationKey = AttributeKey<HardwareAttestationInfo>("HardwareAttestationInfo")

/**
 * Base utility for workstation signature authentication providers.
 */
abstract class BaseWorkstationAuthProvider(config: Config) : AuthenticationProvider(config) {
    class Config(name: String?) : AuthenticationProvider.Config(name)

    protected suspend fun validateTimestampAndHeaders(
        call: ApplicationCall,
        auditor: TelemetryAuditor
    ): Triple<String, String, Long>? {
        val auditContext = call.toAuditRequestContext()
        val hardwareId = call.request.headers["X-Hardware-Id"]
        val signature = call.request.headers["X-Workstation-Signature"]
        val timestamp = call.request.headers["X-Timestamp"]?.toLongOrNull() ?: 0L
        val organizationId = call.request.headers["X-Organization-Id"]
        val workspaceId = call.request.headers["X-Workspace-Id"]

        if (hardwareId == null || signature == null) {
            auditor.logNetworkSecurityEvent(
                triggerEvent = "MISSING_HARDWARE_SECURITY_HEADERS",
                organizationId = organizationId,
                workspaceId = workspaceId,
                deviceId = hardwareId,
                level = TelemetryLevel.WARNING
            )
            logger.warn { "NIST SP 800-53 IA-2: Missing hardware security headers for ${auditContext.endpointRoute} [IP: ${auditContext.clientIp}]" }
            return null
        }

        // Anti-Replay: Verify timestamp is recent (within 5 minutes)
        val now = System.currentTimeMillis()
        if (abs(now - timestamp) > 300_000) {
            auditor.logNetworkSecurityEvent(
                triggerEvent = "SIGNATURE_EXPIRED_CLOCK_DRIFT",
                organizationId = organizationId,
                workspaceId = workspaceId,
                deviceId = hardwareId,
                level = TelemetryLevel.WARNING
            )
            logger.warn { "NIST SP 800-53 SC-23: Signature expired (Clock Drift) for $hardwareId" }
            return null
        }

        return Triple(hardwareId, signature, timestamp)
    }

    protected suspend fun verifyHardwareTrust(
        hardwareId: String,
        incomingFingerprint: String?,
        securityContext: WorkstationSecurityContext,
        actionLabel: String,
        auditor: TelemetryAuditor,
        call: ApplicationCall
    ): Boolean {
        if (securityContext.masterStatus != HardwareMasterStatus.ACTIVE) {
            auditor.logIdentityAccessEvent(
                userId = hardwareId,
                action = actionLabel,
                outcome = "FAILURE",
                failureReason = "Hardware master status: ${securityContext.masterStatus}",
                level = TelemetryLevel.WARNING
            )
            call.respond(
                HttpStatusCode.Forbidden,
                "Hardware master status: ${securityContext.masterStatus}"
            )
            return false
        }

        if (!incomingFingerprint.isNullOrBlank()) {
            val registeredFingerprint = securityContext.fingerprint
            if (registeredFingerprint.isNullOrBlank() || registeredFingerprint != incomingFingerprint) {
                dbQuery {
                    HardwareRegistry.update({ HardwareRegistry.hardwareId eq hardwareId }) {
                        it[HardwareRegistry.fingerprint] = incomingFingerprint
                        it[HardwareRegistry.updatedAt] = Clock.System.now()
                    }
                }
                logger.info { "Auto-attested hardware fingerprint for device $hardwareId to '$incomingFingerprint'" }
            }
        }
        return true
    }
}

/**
 * Unified Workstation Signature Authentication Provider (`device-access-auth`).
 * Validates hardware authenticity via asymmetric signature and FleetDNA token.
 */
class WorkstationSignatureAuthenticationProvider(config: Config) :
    BaseWorkstationAuthProvider(config) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val auditor: TelemetryAuditor by call.application.inject()
        val contextProvider: AuthorizationContextProvider by call.application.inject()

        val rawOrgHeader = call.request.headers["X-Organization-Id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing X-Organization-Id header")
            return
        }
        val organizationId = rawOrgHeader.split(",").firstOrNull()?.trim() ?: rawOrgHeader
        val fleetAuth = call.request.headers["X-Fleet-Auth"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing X-Fleet-Auth header")
            return
        }
        val hardwareFingerprint = call.request.headers["X-Hardware-Fingerprint"]
        val workspaceId = call.request.headers["X-Workspace-Id"]

        val (hardwareId, _, timestamp) = validateTimestampAndHeaders(call, auditor) ?: run {
            call.respond(HttpStatusCode.Unauthorized, "Missing security headers or clock drift")
            return
        }

        if (!contextProvider.verifyFleetToken(organizationId, fleetAuth)) {
            logger.warn { "[AUTH_REJECT] FleetDNA Verification Failed for Path=${call.request.path()}, Org=$organizationId, Device=$hardwareId" }
            auditor.logNetworkSecurityEvent(
                triggerEvent = "FLEET_DNA_VERIFICATION_FAILED",
                organizationId = organizationId,
                deviceId = hardwareId,
                level = TelemetryLevel.WARNING
            )
            call.respond(
                HttpStatusCode.Unauthorized,
                "Organizational trust (FleetDNA) not established"
            )
            return
        }

        val securityContext =
            contextProvider.getHubSecurityContext(organizationId, hardwareId) ?: run {
                logger.warn { "[AUTH_REJECT] Device $hardwareId not registered in HardwareRegistry for Org $organizationId on Path=${call.request.path()}" }
                auditor.logIdentityAccessEvent(
                    action = "WORKSTATION_AUTH",
                    outcome = "FAILURE",
                    failureReason = "Hardware not registered in organization",
                    level = TelemetryLevel.WARNING
                )
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Workstation hardware not registered in organization"
                )
                return
            }

        if (!verifyHardwareTrust(
                hardwareId,
                hardwareFingerprint,
                securityContext,
                "WORKSTATION_AUTH",
                auditor,
                call
            )
        ) {
            return
        }

        auditor.logIdentityAccessEvent(
            action = "WORKSTATION_AUTH",
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )

        context.principal(
            WorkstationDevicePrincipal(
                hardwareId = hardwareId,
                organizationId = organizationId,
                workspaceId = workspaceId
            )
        )
    }
}

/**
 * Login workstation signature authentication provider (`device-login-auth`).
 */
class LoginSignatureAuthenticationProvider(config: Config) : BaseWorkstationAuthProvider(config) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val auditor: TelemetryAuditor by call.application.inject()
        val contextProvider: AuthorizationContextProvider by call.application.inject()

        val workstationPuk = call.request.headers["X-Workstation-Puk"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing X-Workstation-Puk header")
            return
        }

        val hardwareFingerprint = call.request.headers["X-Hardware-Fingerprint"]
        val organizationId = call.request.headers["X-Organization-Id"]
        val workspaceId = call.request.headers["X-Workspace-Id"]
        val sigVersion = call.request.headers["X-Sig-Version"] ?: "1"

        val (hardwareId, signature, timestamp) = validateTimestampAndHeaders(call, auditor) ?: run {
            call.respond(HttpStatusCode.Unauthorized, "Missing security headers or clock drift")
            return
        }

        if (!organizationId.isNullOrBlank()) {
            val orgUuid = runCatching { organizationId.toUuid() }.getOrNull()
            val orgExists = orgUuid != null && dbQuery {
                Organizations.selectAll().where { Organizations.id eq orgUuid }.count() > 0
            }
            if (!orgExists) {
                logger.warn { "[AUTH_REJECT] Organization $organizationId specified in header does not exist for device $hardwareId" }
                auditor.logIdentityAccessEvent(
                    action = "WORKSTATION_AUTH",
                    outcome = "FAILURE",
                    failureReason = "Organization not found",
                    level = TelemetryLevel.WARNING
                )
                call.respond(HttpStatusCode.Unauthorized, "Organization not found or invalid organization identity")
                return
            }
        }

        val securityContext = organizationId?.let { orgId ->
            contextProvider.getHubSecurityContext(orgId, hardwareId)
        }

        val isRecognizedBase = securityContext != null
        val registeredFingerprint = securityContext?.fingerprint
        val hasFingerprintMismatch = registeredFingerprint != null &&
                hardwareFingerprint != null &&
                registeredFingerprint != hardwareFingerprint

        val attestationInfo = HardwareAttestationInfo(
            isRecognized = isRecognizedBase,
            hasFingerprintMismatch = hasFingerprintMismatch,
            masterStatus = securityContext?.masterStatus,
            registeredFingerprint = registeredFingerprint,
            incomingFingerprint = hardwareFingerprint
        )
        call.attributes.put(HardwareAttestationKey, attestationInfo)

        if (hasFingerprintMismatch) {
            auditor.logNetworkSecurityEvent(
                triggerEvent = "LOGIN_HARDWARE_FINGERPRINT_MISMATCH",
                organizationId = organizationId,
                workspaceId = workspaceId,
                deviceId = hardwareId,
                level = TelemetryLevel.WARNING
            )
            logger.warn { "NIST SP 800-53 SI-7: Hardware fingerprint mismatch during login for $hardwareId. Forcing MFA / Re-attestation." }
        }

        val isRecognized = isRecognizedBase && !hasFingerprintMismatch

        val claimWsId = workspaceId ?: ""
        val claimOrgId = organizationId ?: ""

        val payload1 = "$hardwareId:$claimWsId:$timestamp:"
        val payload2 = "$hardwareId:$claimOrgId:$timestamp:"
        val payload3 = "$hardwareId::$timestamp:"

        val isValid = SignatureVerifier.verify(
            publicKeyBase64 = workstationPuk,
            data = payload1.encodeToByteArray(),
            signatureBase64 = signature
        ) || SignatureVerifier.verify(
            publicKeyBase64 = workstationPuk,
            data = payload2.encodeToByteArray(),
            signatureBase64 = signature
        ) || SignatureVerifier.verify(
            publicKeyBase64 = workstationPuk,
            data = payload3.encodeToByteArray(),
            signatureBase64 = signature
        )

        if (isValid) {
            if (!organizationId.isNullOrBlank() && hardwareId.isNotBlank()) {
                val hwModel = call.request.headers["X-Hardware-Model"]
                val hwOS = call.request.headers["X-Hardware-OS"]
                val hwArch = call.request.headers["X-Hardware-Arch"]
                val hwName = call.request.headers["X-Hardware-Name"]

                runCatching {
                    val orgUuid = organizationId.toUuid()
                    val orgExists = dbQuery {
                        Organizations.selectAll().where { Organizations.id eq orgUuid }.count() > 0
                    }
                    if (orgExists) {
                        dbQuery {
                            HardwareRegistry.upsert {
                                it[HardwareRegistry.hardwareId] = hardwareId
                                it[HardwareRegistry.organizationId] = orgUuid
                                it[HardwareRegistry.masterStatus] = HardwareMasterStatus.ACTIVE
                                it[HardwareRegistry.fingerprint] = hardwareFingerprint
                                it[HardwareRegistry.model] = hwModel
                                it[HardwareRegistry.osName] = hwOS
                                it[HardwareRegistry.architecture] = hwArch
                                it[HardwareRegistry.publicKey] = workstationPuk
                                it[HardwareRegistry.displayName] = hwName
                                it[HardwareRegistry.updatedAt] = Clock.System.now()
                            }
                        }
                    } else {
                        logger.warn { "NIST SP 800-53 IA-2: Organization $organizationId not found in DB for hardware $hardwareId. Skipping HardwareRegistry upsert." }
                    }
                }.onFailure { e ->
                    logger.warn(e) { "Failed to register hardware standing in HardwareRegistry for device $hardwareId" }
                }
            }

            if (isRecognized) {
                auditor.logIdentityAccessEvent(
                    userId = hardwareId,
                    action = "DEVICE_ATTESTATION",
                    outcome = "SUCCESS",
                    level = TelemetryLevel.INFO
                )
                logger.info { "NIST SP 800-53 IA-2: Recognized trusted device $hardwareId attempting login. Skipping OTP verification." }
                context.principal(
                    WorkstationDevicePrincipal(
                        hardwareId = hardwareId,
                        organizationId = organizationId,
                        workspaceId = workspaceId
                    )
                )
            } else {
                auditor.logIdentityAccessEvent(
                    action = "DEVICE_ATTESTATION_PENDING_MFA",
                    outcome = "SUCCESS",
                    level = TelemetryLevel.INFO
                )
                logger.info { "NIST SP 800-53 IA-2: Complete new device $hardwareId requesting login into hub. MFA required." }
                context.principal(
                    WorkstationDevicePrincipal(
                        hardwareId = hardwareId,
                        organizationId = organizationId,
                        workspaceId = workspaceId
                    )
                )
            }
        } else {
            auditor.logIdentityAccessEvent(
                action = "DEVICE_ATTESTATION",
                outcome = "FAILURE",
                failureReason = "Invalid workstation attestation signature (version $sigVersion)",
                level = TelemetryLevel.WARNING
            )
            logger.warn { "NIST SP 800-53 IA-2: Invalid workstation attestation signature for $hardwareId (ver: $sigVersion)" }
            call.respond(HttpStatusCode.Unauthorized, "Invalid workstation attestation signature")
        }
    }
}
