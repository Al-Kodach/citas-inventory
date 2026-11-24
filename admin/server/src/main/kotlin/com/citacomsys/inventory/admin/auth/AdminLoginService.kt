package com.citacomsys.inventory.admin.auth

import com.citacomsys.api.auth.SessionMetadata
import com.citacomsys.auth.session.AuthenticationOrchestrator
import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.core.security.service.MfaAuthInfo
import com.citacomsys.core.security.service.MfaService
import com.citacomsys.core.security.service.SecurityBreachException
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.security.service.SecurityTrackableEvent
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.inventory.api.user.PlatformAdminRepository
import com.citacomsys.inventory.api.user.model.PlatformAdmin
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.types.*
import mu.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

internal class AdminLoginService(
    private val adminRepository: PlatformAdminRepository,
    private val passwordHasher: PasswordHasher,
    private val mfaService: MfaService,
    private val sessionIssuer: AuthenticationOrchestrator,
    private val auditor: TelemetryAuditor,
    private val securityThrottler: SecurityThrottlerService
) {

    suspend fun authenticate(
        identity: String,
        password: String,
        currentDeviceId: String,
        sessionMetadata: SessionMetadata
    ): RemoteAuthResult {
        val admin = adminRepository.findByIdentifier(identity).getOrNull()
            ?: throw IllegalArgumentException("Invalid administrator credentials")

        if (admin.status != UserStatus.ACTIVE) {
            throw IllegalStateException("Administrator account is inactive or suspended")
        }

        val throttleKey = "admin_login_$identity"
        try {
            securityThrottler.attempt(
                key = throttleKey,
                event = SecurityTrackableEvent.FAILED_PASSWORD_ATTEMPT,
                maxAttempts = 5
            )
            securityThrottler.attempt(
                key = "ip_${sessionMetadata.ipAddress}",
                event = SecurityTrackableEvent.FAILED_PASSWORD_ATTEMPT,
                maxAttempts = 20
            )
        } catch (e: SecurityBreachException) {
            auditor.logIdentityAccessEvent(
                userId = admin.id,
                action = "ADMIN_LOGIN_THROTTLED",
                outcome = "FAILURE",
                failureReason = e.message,
                level = TelemetryLevel.WARNING
            )
            throw IllegalArgumentException(e.message ?: "Rate limit exceeded")
        }

        val isPasswordValid = passwordHasher.verify(password, admin.passwordHash)
        if (!isPasswordValid) {
            securityThrottler.recordEvent(
                key = throttleKey,
                event = SecurityTrackableEvent.FAILED_PASSWORD_ATTEMPT
            )
            auditor.logIdentityAccessEvent(
                userId = admin.id,
                action = "ADMIN_LOGIN_FAILED",
                outcome = "FAILURE",
                failureReason = "Invalid password",
                level = TelemetryLevel.WARNING
            )
            throw IllegalArgumentException("Invalid administrator credentials")
        }

        val activeHardwareId = admin.activeHardwareId
        val isDev = System.getProperty("ENV")?.lowercase()?.startsWith("dev") == true


        return when {
            // Unregistered / New Device (activeHardwareId is null) -> Issue OTP & Return MfaRequired challenge
            activeHardwareId == null -> {
                val account = MfaAuthInfo(
                    userId = admin.id,
                    username = admin.username,
                    email = admin.email,
                    phone = PhoneNumber(admin.phone),
                    purpose = OtpPurpose.WORKSPACE_BINDING
                )

                if (isDev.not()) {
                    mfaService.authAccount(account).getOrThrow()
                }

                auditor.logIdentityAccessEvent(
                    userId = admin.id,
                    action = "ADMIN_DEVICE_BINDING_CHALLENGE",
                    outcome = "PENDING",
                    level = TelemetryLevel.INFO
                )

                RemoteAuthResult.MfaRequired(
                    AuthChallengeContext(
                        userId = admin.id,
                        organizationId = SystemActors.SYSTEM_ID,
                        accountMasked = PhoneNumber(admin.phone).mask,
                        otpPurpose = OtpPurpose.WORKSPACE_BINDING.name,
                        channel = MFAChannel.SMS
                    )
                )
            }

            // Recognized Device (activeHardwareId matches currentDeviceId) -> SKIP MFA!
            activeHardwareId == currentDeviceId -> {
                auditor.logIdentityAccessEvent(
                    userId = admin.id,
                    action = "ADMIN_MFA_BYPASSED_RECOGNIZED_DEVICE",
                    outcome = "SUCCESS",
                    level = TelemetryLevel.INFO
                )

                val user = toUser(admin)
                val session = sessionIssuer.createAuthenticatedSession(
                    user = user,
                    workspaceId = null,
                    metadata = sessionMetadata
                )

                RemoteAuthResult.Success(session)
            }

            // Different Device Bound (Single device enforcement violation)
            else -> {
                auditor.logIdentityAccessEvent(
                    userId = admin.id,
                    action = "ADMIN_DEVICE_MISMATCH_LOCKED",
                    outcome = "FAILURE",
                    failureReason = "Admin bound to another device ($activeHardwareId)",
                    level = TelemetryLevel.ERROR
                )
                RemoteAuthResult.DeviceLocked
            }
        }
    }

    suspend fun verifyMfa(
        id: String,
        otpCode: String,
        currentDeviceId: String,
        sessionMetadata: SessionMetadata
    ): AuthenticatedSession {
        val admin = adminRepository.findById(id).getOrNull()
            ?: throw IllegalArgumentException("Admin not found")

        val throttleKey = "admin_mfa_$id"
        try {
            securityThrottler.attempt(
                key = throttleKey,
                event = SecurityTrackableEvent.FAILED_OTP_ATTEMPT,
                maxAttempts = 3
            )
        } catch (e: SecurityBreachException) {
            auditor.logIdentityAccessEvent(
                userId = admin.id,
                action = "ADMIN_MFA_THROTTLED",
                outcome = "FAILURE",
                failureReason = e.message,
                level = TelemetryLevel.WARNING
            )
            throw IllegalArgumentException(e.message ?: "Rate limit exceeded")
        }

        val isDev = System.getProperty("ENV")?.lowercase()?.startsWith("dev") == true

        if (isDev.not()) {
            val verifyResult = mfaService.verify(admin.id, otpCode, OtpPurpose.WORKSPACE_BINDING)
            if (verifyResult.isFailure) {
                securityThrottler.recordEvent(
                    key = throttleKey,
                    event = SecurityTrackableEvent.FAILED_OTP_ATTEMPT
                )
                verifyResult.getOrThrow()
            }
        }

        // Register hardware in HardwareRegistry FIRST to satisfy foreign key constraint!
        registerHardwareStanding(currentDeviceId)

//        // Bind device to platform_admins.active_hardware_id upon successful verification
        adminRepository.updateActiveHardware(admin.id, currentDeviceId)

        auditor.logIdentityAccessEvent(
            userId = admin.id,
            action = "ADMIN_DEVICE_BOUND_AND_AUTHENTICATED",
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )

        val user = toUser(admin)
        return sessionIssuer.createAuthenticatedSession(
            user = user,
            workspaceId = null,
            metadata = sessionMetadata
        )
    }

    suspend fun bindDevice(
        identifier: String,
        deviceId: String,
        otpCode: String,
        requestIp: String
    ): BindDeviceResponse {
        val admin = adminRepository.findByIdentifier(identifier).getOrNull()
            ?: throw IllegalArgumentException("Admin not found")

        mfaService.verify(admin.id, otpCode, OtpPurpose.WORKSPACE_BINDING).getOrThrow()

        // Register hardware in HardwareRegistry FIRST
        registerHardwareStanding(deviceId)

        adminRepository.updateActiveHardware(admin.id, deviceId)

        auditor.logIdentityAccessEvent(
            userId = admin.id,
            action = "ADMIN_DEVICE_BOUND",
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )

        return BindDeviceResponse.Success(
            deviceToken = "admin-device-bound"
        )
    }

    suspend fun requestDeviceMigration(identity: String, password: String): Result<Unit> =
        runCatching {
            val admin = adminRepository.findByIdentifier(identity).getOrNull()
                ?: throw IllegalArgumentException("Admin not found")
            if (!passwordHasher.verify(password, admin.passwordHash)) {
                throw IllegalArgumentException("Invalid password")
            }
            mfaService.send(
                userId = admin.id,
                phone = PhoneNumber(admin.phone),
                purpose = OtpPurpose.WORKSPACE_BINDING
            ).getOrThrow()
        }

    suspend fun confirmDeviceMigration(
        identity: String,
        otpCode: String,
        newDeviceId: String
    ): Result<Unit> = runCatching {
        val admin = adminRepository.findByIdentifier(identity).getOrNull()
            ?: throw IllegalArgumentException("Admin not found")
        mfaService.verify(admin.id, otpCode, OtpPurpose.WORKSPACE_BINDING).getOrThrow()

        // Register hardware in HardwareRegistry FIRST
        registerHardwareStanding(newDeviceId)

        adminRepository.updateActiveHardware(admin.id, newDeviceId)
        auditor.logIdentityAccessEvent(
            userId = admin.id,
            action = "ADMIN_DEVICE_MIGRATED",
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )
    }

    private suspend fun registerHardwareStanding(currentHardwareId: String) = dbQuery {
        HardwareRegistry.upsert {
            it[hardwareId] = currentHardwareId
            it[organizationId] = SystemActors.SYSTEM_ID.toUuid()
            it[masterStatus] = HardwareMasterStatus.ACTIVE
            it[updatedAt] = Clock.System.now()
        }
        logger.info { "Hardware $currentHardwareId registered/verified in HardwareRegistry for system platform admin" }
    }

    private fun toUser(admin: PlatformAdmin): User {
        return User(
            id = admin.id,
            organizationId = SystemActors.SYSTEM_ID,
            name = admin.name,
            username = admin.username,
            phone = PhoneNumber(admin.phone),
            isPhoneVerified = true,
            email = admin.email,
            passwordHash = admin.passwordHash,
            userType = UserType.PLATFORM,
            mfaEnabled = admin.mfaEnabled,
            status = admin.status,
            role = admin.role
        )
    }
}
