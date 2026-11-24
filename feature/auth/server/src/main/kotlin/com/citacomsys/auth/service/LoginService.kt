package com.citacomsys.auth.service

import com.citacomsys.api.auth.SessionMetadata
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.auth.session.AuthenticationOrchestrator
import com.citacomsys.core.domain.result.AuthenticationResult
import com.citacomsys.core.domain.result.UserAuthenticationException
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.core.security.plugin.HardwareAttestationInfo
import com.citacomsys.core.security.service.MfaAuthInfo
import com.citacomsys.core.security.service.MfaService
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.types.*
import org.jetbrains.exposed.v1.jdbc.upsert
import mu.KotlinLogging
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

internal class LoginService(
    private val authenticationService: UserAuthenticationService,
    private val mfaService: MfaService,
    private val sessionIssuer: AuthenticationOrchestrator,
    private val userRepository: UserPort,
    private val auditor: TelemetryAuditor
) {

    /**
     * Authenticates a user into the Organization Hub.
     * Establishes the physical hardware identity and global security standing.
     */
    suspend fun authenticate(command: Command): Result<RemoteAuthResult> = runCatching {
        logger.info("Hub Login request for Identity: ${command.userId} on Hardware: ${command.session.deviceId}")

        val authResult = try {
            val user = authenticationService.execute(
                identity = command.userId,
                password = command.password
            )
            AuthenticationResult.Success(user)
        } catch (e: UserAuthenticationException) {
            e.result
        }

        if (authResult is AuthenticationResult.Success) {
            if (command.hardwareAttestation?.masterStatus == HardwareMasterStatus.REVOKED) {
                throw IllegalStateException("Access Denied: This physical hardware is globally locked.")
            }
        }

        when (authResult) {
            is AuthenticationResult.Success -> {
                val user = authResult.user

                // Enforce workspace & POS terminal binding check if activeWorkspaceId is explicitly requested
                if (!command.workspaceId.isNullOrBlank()) {
                    val isOwnerOrAdmin = user.role.equals("OWNER", ignoreCase = true) ||
                            user.role.equals("TENANT_OWNER", ignoreCase = true) ||
                            user.role.equals("ORGANIZATION_ADMIN", ignoreCase = true) ||
                            user.userType == UserType.SYSTEM ||
                            user.userType == UserType.PLATFORM

                    if (!isOwnerOrAdmin) {
                        val bindings = userRepository.getBindingsForUserId(user.id, user.organizationId)
                        val hasBinding = bindings.any { it.workspaceId == command.workspaceId && it.isActive }
                        if (!hasBinding) {
                            auditor.logIdentityAccessEvent(
                                userId = user.id,
                                action = "LOGIN_ATTEMPT_WORKSPACE_UNAUTHORIZED",
                                outcome = "FAILURE",
                                failureReason = "User not bound to workspace ${command.workspaceId}",
                                level = TelemetryLevel.WARNING
                            )
                            throw UserAuthenticationException(AuthenticationResult.AccountBlocked("You are not authorized or bound to access this POS terminal or workspace."))
                        }
                    }
                }

                // Check device recognition and fingerprint status via authoritative attestation plugin info
                val isDeviceRecognized = command.hardwareAttestation?.isRecognized ?: false
                val hasFingerprintMismatch = command.hardwareAttestation?.hasFingerprintMismatch ?: false
                // Enforce MFA if user has MFA enabled, or if device is unrecognized/has fingerprint mismatch
                val requireMfa = user.mfaEnabled || !isDeviceRecognized || hasFingerprintMismatch

                if (hasFingerprintMismatch) {
                    auditor.logIdentityAccessEvent(
                        userId = user.id,
                        action = "HARDWARE_FINGERPRINT_MISMATCH_CHALLENGE",
                        outcome = "PENDING",
                        level = TelemetryLevel.WARNING
                    )
                }

                if (isDeviceRecognized) {
                    auditor.logIdentityAccessEvent(
                        userId = user.id,
                        action = "MFA_BYPASSED_RECOGNIZED_DEVICE",
                        outcome = "SUCCESS",
                        level = TelemetryLevel.INFO
                    )
                }

                if (requireMfa) {
                    val channel = sendOtp(
                        userId = user.id,
                        phone = user.phone,
                        email = user.email,
                        username = user.username,
                        purpose = OtpPurpose.MFA_CHALLENGE
                    )

                    auditor.logIdentityAccessEvent(
                        userId = user.id,
                        action = "MFA_CHALLENGE_ISSUED",
                        outcome = "PENDING",
                        level = TelemetryLevel.INFO
                    )

                    RemoteAuthResult.MfaRequired(
                        AuthChallengeContext(
                            userId = user.id,
                            organizationId = user.organizationId,
                            accountMasked = user.emailMask ?: user.phoneMask,
                            otpPurpose = OtpPurpose.MFA_CHALLENGE.name,
                            channel = channel
                        )
                    )
                } else {
                    // Register / upsert hardware into registry now that trust/MFA is verified
                    registerHardwareStanding(user, command.session.deviceId)

                    val session = sessionIssuer.createAuthenticatedSession(
                        user = user,
                        workspaceId = command.workspaceId,
                        metadata = command.session
                    )

                    auditor.logIdentityAccessEvent(
                        userId = user.id,
                        action = "HUB_SESSION_ESTABLISHED",
                        outcome = "SUCCESS",
                        level = TelemetryLevel.INFO
                    )

                    RemoteAuthResult.Success(session)
                }
            }

            is AuthenticationResult.AccountVerificationRequired -> {
                val channel = sendOtp(
                    userId = authResult.userId,
                    phone = authResult.phone,
                    username = authResult.username,
                    email = authResult.email,
                    purpose = OtpPurpose.ACCOUNT_VERIFICATION
                )

                RemoteAuthResult.VerificationRequired(
                    AuthChallengeContext(
                        userId = authResult.userId,
                        organizationId = authResult.organizationId,
                        accountMasked = authResult.accMasked,
                        otpPurpose = OtpPurpose.ACCOUNT_VERIFICATION.name,
                        channel = channel
                    )
                )
            }

            is AuthenticationResult.PhoneVerificationRequired -> {
                val channel = sendOtp(
                    userId = authResult.userId,
                    phone = authResult.phone,
                    username = authResult.username,
                    email = authResult.email,
                    purpose = OtpPurpose.USER_VERIFICATION
                )

                RemoteAuthResult.VerificationRequired(
                    AuthChallengeContext(
                        userId = authResult.userId,
                        organizationId = authResult.organizationId,
                        accountMasked = authResult.accMasked,
                        otpPurpose = OtpPurpose.USER_VERIFICATION.name,
                        channel = channel
                    )
                )
            }

            is AuthenticationResult.Failure -> throw UserAuthenticationException(authResult)
        }

    }

    private suspend fun registerHardwareStanding(user: User, currentHardwareId: String) = dbQuery {
        HardwareRegistry.upsert {
            it[hardwareId] = currentHardwareId
            it[organizationId] = user.organizationId.toUuid()
            it[masterStatus] = HardwareMasterStatus.ACTIVE
            it[updatedAt] = Clock.System.now()
        }
        logger.info { "Hardware $currentHardwareId registered/verified for organization ${user.organizationId}" }
    }

    private suspend fun sendOtp(
        userId: String,
        username: String,
        phone: PhoneNumber?,
        email: String?,
        purpose: OtpPurpose,
    ): MFAChannel {
        val info = MfaAuthInfo(
            userId = userId,
            username = username,
            email = email,
            phone = phone,
            purpose = purpose
        )
        return mfaService.authAccount(info).getOrThrow()
    }

    data class Command(
        val userId: String,
        val password: String,
        val session: SessionMetadata,
        val workspaceId: String? = null,
        val hardwareAttestation: HardwareAttestationInfo? = null
    )
}
