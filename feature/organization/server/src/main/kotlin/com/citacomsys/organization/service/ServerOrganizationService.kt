package com.citacomsys.organization.service

import com.citacomsys.api.auth.SessionMetadata
import com.citacomsys.api.auth.service.SessionService
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.api.organization.model.OwnerRegistrationDetails
import com.citacomsys.api.organization.model.OrganizationRegistrationRequest
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.api.organization.model.UserSessionDto
import com.citacomsys.api.organization.model.VerificationResult
import com.citacomsys.api.organization.model.WorkstationBindingDto
import com.citacomsys.api.organization.model.WorkstationRecoveryRequest
import com.citacomsys.api.organization.model.WorkstationRecoveryResult
import com.citacomsys.api.workspace.model.WorkspaceDetails
import com.citacomsys.kernel.model.*
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.persistence.table.tenant.AppBrandingTable
import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.core.security.service.MfaService
import com.citacomsys.feature.api.telemetry.telemetryContext
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.types.*
import com.citacomsys.inventory.api.user.UserRepository
import kotlinx.coroutines.currentCoroutineContext
import org.jetbrains.exposed.v1.jdbc.upsert
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import kotlin.time.Clock

internal class ServerOrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val auditor: TelemetryAuditor,
    private val passwordHasher: PasswordHasher,
    private val mfaService: MfaService,
    private val sessionService: SessionService,
    private val notificationService: NotificationService
) : OrganizationService {
    private val logger = KotlinLogging.logger { }

    val isDevelopment = System.getenv("ENV") != "PRODUCTION"

    /**
     * Phase 1: Register Organization and its Primary Owner account.
     * This establishes the legal parent entity. Workspace creation happens in Phase 2.
     */
    override suspend fun registerOrganization(
        request: OrganizationRegistrationRequest
    ): OrganizationRegistrationResult = dbQuery {
        val context = currentCoroutineContext().telemetryContext
        logger.info { "Starting root organization registration: ${request.organizationName} on device: ${context?.deviceId}" }

        // Identity Conflict Check
        val existingByPhone = userRepository.findByPhone(request.owner.phone.value).getOrNull()
        if (existingByPhone != null) {
            error("This phone number is already registered with an organization.")
        }

        if (organizationRepository.existsByName(request.organizationName)) {
            error("An organization with this ID is already registered.")
        }

        val businessCode = generateUniqueBusinessCode(request.organizationName)
        val fleetDna = "dna_${generateUUID().take(12)}"

        // Create the Legal Parent entity
        val organization = organizationRepository.create(
            name = request.organizationName,
            businessCode = businessCode,
            fleetDna = fleetDna,
            brandingConfig = request.brand,
            email = request.owner.email,
            phone = request.owner.phone.value
        ).getOrThrow()

        // Create the Staff Owner account at Org level
        val owner = createOwner(organization.id, request.owner)

        // Update Organization with confirmed Owner anchor
        organizationRepository.updateOwnerId(organization.id, owner.id)

        // Register onboarding physical hardware as Mother Hub / Primary active node for this Organization
        registerOnboardingHardware(
            organization.id,
            context?.deviceId,
            fingerprint = null,
            puk = null
        )

        // Dispatch Welcome & Organization Registration Details Email
        val ownerEmail = request.owner.email
        if (ownerEmail.isNotBlank()) {
            runCatching {
                notificationService.sendOrganizationWelcomeEmail(
                    email = ownerEmail,
                    phone = request.owner.phone,
                    orgName = organization.name,
                    businessCode = businessCode,
                    ownerName = owner.name
                )
            }.onFailure {
                logger.error { "Failed to send organization welcome email to $ownerEmail: ${it.message}" }
            }
        }

        // Onboarding OTP Verification (Controlled via ENV "ENABLE_ONBOARDING_OTP")
        if (!isDevelopment) {
            mfaService.send(
                userId = owner.id,
                phone = owner.phone,
                purpose = OtpPurpose.ACCOUNT_VERIFICATION
            ).onFailure {
                logger.error { "Failed to send onboarding OTP to ${owner.phone.value}: ${it.message}" }
            }
        }

        auditor.logApplicationEvent(
            action = "ORGANIZATION_REGISTERED",
            entityType = EntityType.TENANT.name,
            entityId = organization.id,
            summary = "New organization established: ${request.organizationName} ($businessCode)",
            level = TelemetryLevel.INFO
        )

        OrganizationRegistrationResult(
            organizationId = organization.id,
            ownerId = owner.id,
            ownerPhoneMasked = owner.phone.mask
        )
    }


    override suspend fun verifyOwner(
        ownerId: String,
        code: String,
        fingerprint: String?,
        wsPublicKey: String?,
    ): VerificationResult {
        val context = currentCoroutineContext().telemetryContext

        // Onboarding OTP Verification (Controlled via ENV "ENABLE_ONBOARDING_OTP")
        if (!isDevelopment) {
            mfaService.verify(
                ownerId,
                code,
                OtpPurpose.ACCOUNT_VERIFICATION
            ).getOrThrow()
        }

        val user = userRepository.findById(ownerId).getOrNull()
            ?: throw IllegalArgumentException("Owner not found")

        val org = organizationRepository.get(user.organizationId).getOrThrow()

        val owner = userRepository.save(
            user.copy(
                status = UserStatus.ACTIVE,
                isPhoneVerified = true
            )
        )

        organizationRepository.updateStatus(org.id, status = OrgStatus.ACTIVE)

        // Register onboarding physical hardware as Mother Hub / Primary active node for this Organization
        registerOnboardingHardware(
            org.id,
            context?.deviceId,
            fingerprint,
            wsPublicKey
        )

        if (!user.email.isNullOrBlank()) {
            runCatching {
                notificationService.sendOrganizationWelcomeEmail(
                    email = user.email,
                    phone = user.phone,
                    orgName = org.name,
                    businessCode = org.businessCode,
                    ownerName = owner.name
                )
            }.onFailure {
                logger.error { "Failed to send organization welcome email to ${user.email}: ${it.message}" }
            }
        }

        val config = OrganizationRuntimeConfig(
            organizationId = org.id,
            name = org.name,
            businessCode = org.businessCode,
            fleetDna = org.fleetDna,
            status = org.status.name,
            branding = org.brand,
            metrics = OrganizationMetrics.EMPTY.copy(totalStaffAccounts = 1)
        )

        val session = SessionMetadata(
            ipAddress = context?.clientIp ?: "Unknown IpAddress",
            userAgent = context?.userAgent,
            deviceId = context?.deviceId ?: "unknown device",
            fingerprint = fingerprint,
            publicKey = wsPublicKey,
        )

        val tokens = sessionService.create(owner, null, session)
        return VerificationResult(config, tokens, owner)
    }

    override suspend fun recoverWorkstation(
        request: WorkstationRecoveryRequest,
        fingerprint: String?,
        wsPublicKey: String?
    ): WorkstationRecoveryResult = dbQuery {
        val context = currentCoroutineContext().telemetryContext
        logger.info { "Workstation recovery requested for business code: ${request.businessCode}, activeHardwareId: ${request.hardwareId}" }

        val org = organizationRepository.findByBusinessCode(request.businessCode).getOrNull()
            ?: throw IllegalArgumentException("Organization not found for business code")

        val owner = userRepository.findOwner(org.id)
            ?: throw IllegalStateException("Organization owner account not found")

        if (!passwordHasher.verify(request.recoveryCode, owner.passwordHash)) {
            auditor.logApplicationEvent(
                action = "WORKSTATION_RECOVERY_FAILED",
                entityType = EntityType.TENANT.name,
                entityId = org.id,
                summary = "Failed workstation recovery attempt due to invalid recovery code",
                level = TelemetryLevel.WARNING,
                organizationIdOverride = org.id
            )
            throw IllegalArgumentException("Invalid recovery code or credentials")
        }

        HardwareRegistry.upsert {
            it[this.hardwareId] = request.hardwareId
            it[this.organizationId] = org.id.toUuid()
            it[this.masterStatus] = HardwareMasterStatus.ACTIVE
            it[this.securityReason] = null
            it[this.updatedAt] = Clock.System.now()
            it[this.createdAt] = Clock.System.now()
        }

        val config = OrganizationRuntimeConfig(
            organizationId = org.id,
            name = org.name,
            businessCode = org.businessCode,
            fleetDna = org.fleetDna,
            status = org.status.name,
            branding = org.brand,
            metrics = OrganizationMetrics.EMPTY.copy(totalStaffAccounts = 1)
        )

        val session = SessionMetadata(
            ipAddress = context?.clientIp ?: "Unknown IpAddress",
            userAgent = context?.userAgent,
            deviceId = request.hardwareId,
            fingerprint = fingerprint,
            publicKey = wsPublicKey,
        )

        val tokens = sessionService.create(owner, null, session)

        auditor.logApplicationEvent(
            action = "WORKSTATION_RECOVERED",
            entityType = EntityType.TENANT.name,
            entityId = org.id,
            summary = "Workstation ${request.hardwareId} successfully recovered for organization ${org.name}",
            level = TelemetryLevel.INFO,
            organizationIdOverride = org.id
        )

        WorkstationRecoveryResult(config, tokens, owner)
    }

    override suspend fun updateProfile(
        orgId: String,
        name: String,
        email: String?,
        phone: String?
    ): Result<Unit> = runCatching {
        dbQuery {
            val organization = organizationRepository.get(orgId).getOrNull()
                ?: throw IllegalArgumentException("Organization not found")

            organizationRepository.updateProfile(
                organizationId = organization.id,
                name = name,
                email = email,
                phone = phone
            )
        }
    }

    override suspend fun deleteBrand(orgId: String) {
        dbQuery {
            AppBrandingTable.deleteWhere { AppBrandingTable.id eq orgId.toUuid() }
        }
    }

    override suspend fun saveBranding(
        orgId: String,
        brandingConfig: BrandingConfig
    ) {
        dbQuery {
            AppBrandingTable.upsert {
                it[this.id] = orgId.toUuid()
                it[appName] = brandingConfig.appName
                it[primaryColor] = brandingConfig.primaryColor
                it[accentColor] = brandingConfig.accentColor
                it[tertiaryColor] = brandingConfig.tertiaryColor
                it[errorColor] = brandingConfig.errorColor
                it[logoUrl] = brandingConfig.logoUrl
                it[defaultDarkMode] = brandingConfig.defaultDarkMode
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun getWorkspaceDetails(workspaceId: String): Result<WorkspaceDetails> =
        runCatching {
            val context = currentCoroutineContext().telemetryContext
            val orgId = context?.organizationId ?: error("Organization context not active")
            organizationRepository.getWorkspaceDetails(workspaceId, orgId)
        }

    private fun registerOnboardingHardware(
        organizationId: String,
        hardwareId: String?,
        fingerprint: String?,
        puk: String?
    ) {
        if (hardwareId.isNullOrBlank() || hardwareId == "unknown device") {
            logger.warn { "No valid hardware ID found during onboarding for Org $organizationId. (hardwareId: $hardwareId)" }
            return
        }

        HardwareRegistry.upsert {
            it[this.hardwareId] = hardwareId
            it[this.organizationId] = organizationId.toUuid()
            it[this.masterStatus] = HardwareMasterStatus.ACTIVE
            if (!fingerprint.isNullOrBlank()) {
                it[this.fingerprint] = fingerprint
            }
            if (puk != null) {
                it[publicKey] = puk
            }
            it[this.updatedAt] = Clock.System.now()
            it[this.createdAt] = Clock.System.now()
        }
        logger.info { "Registered onboarding hardware $hardwareId with fingerprint $fingerprint as Mother Hub for Organization $organizationId" }
    }

    private suspend fun createOwner(
        organizationId: String,
        details: OwnerRegistrationDetails
    ): User {
        val hashedPassword = passwordHasher.hash(details.password)

        return userRepository.save(
            User(
                id = generateUUID(),
                organizationId = organizationId,
                name = details.name,
                username = details.username,
                phone = details.phone,
                email = details.email,
                role = TenantUserRole.OWNER.name,
                status = UserStatus.REQUIRED_PHONE_VERIFICATION,
                userType = UserType.TENANT,
                passwordHash = hashedPassword,
                isPhoneVerified = false,
                mfaEnabled = true
            )
        )
    }

    private suspend fun generateUniqueBusinessCode(name: String): String {
        repeat(10) {
            val code = BusinessCodeGenerator.generate(name)
            if (!organizationRepository.existsByBusinessCode(code)) return code
        }
        error("Unable to generate unique business code")
    }

    override suspend fun getWorkstationBindings(orgId: String): Result<List<WorkstationBindingDto>> =
        runCatching {
            organizationRepository.getWorkstationBindings(orgId)
        }

    override suspend fun revokeWorkstationBinding(bindingId: String): Result<Unit> = runCatching {
        dbQuery {
            organizationRepository.revokeWorkstationBinding(bindingId)
        }
    }

    override suspend fun getUserSessions(orgId: String): Result<List<UserSessionDto>> =
        runCatching {
            val pageResult = sessionService.fetch(PageQuery(page = 0, size = 500), orgId)
            pageResult.items.map { view ->
                UserSessionDto(
                    id = view.id,
                    userId = view.userId,
                    organizationId = view.organizationId,
                    workspaceId = view.workspaceId,
                    deviceId = view.deviceId,
                    createdAt = view.createdAt,
                    lastUsedAt = view.lastUsedAt,
                    revoked = view.revoked,
                    revokedAt = null,
                    revokeReason = null,
                    lastIp = view.lastIp,
                    userAgent = view.userAgent,
                    platform = view.platform,
                    appVersion = view.appVersion
                )
            }
        }

    override suspend fun revokeUserSession(sessionId: String, reason: String?): Result<Unit> =
        runCatching {
            sessionService.revokeSession(sessionId, reason ?: "Revoked by admin")
        }
}
