package com.citacomsys.auth.usecase

import com.citacomsys.core.security.service.MfaService
import com.citacomsys.api.auth.SessionMetadata
import com.citacomsys.auth.session.AuthenticationOrchestrator
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.types.AuthenticatedSession

import mu.KotlinLogging

class MfaVerificationUseCase(
    private val userRepository: UserPort,
    private val organizationRepository: OrganizationRepository,
    private val sessionIssuer: AuthenticationOrchestrator,
    private val mfaService: MfaService
) {
    private val logger = KotlinLogging.logger { }

    suspend fun execute(command: Command): Result<AuthenticatedSession> = runCatching {
        val user = userRepository.findById(command.userId).getOrThrow()
        val resolvedUserId = user.id

        val isDev = System.getenv("ENV")?.lowercase() == "dev"

        runCatching {
            mfaService.verify(
                userId = user.id,
                code = command.code,
                purpose = command.purpose
            ).getOrThrow()
        }.onFailure { e ->
            if (isDev) {
                logger.warn { "Dev Mode active (ENV=dev): Bypassing OTP validation failure: ${e.message}" }
            } else {
                throw e
            }
        }

        // Activate User and Phone Verification ONLY for account or user verification flows
        if (command.purpose == OtpPurpose.ACCOUNT_VERIFICATION || command.purpose == OtpPurpose.USER_VERIFICATION) {
            userRepository.activateUser(resolvedUserId)
            userRepository.markPhoneNumberAsVerified(resolvedUserId)
        }

        // If this user is the organization owner, activate the parent organization from PENDING_VERIFICATION to ACTIVE
        if (command.purpose == OtpPurpose.ACCOUNT_VERIFICATION && (user.role == "OWNER" || user.role == "TENANT_OWNER")) {
            runCatching {
                organizationRepository.updateStatus(user.organizationId, OrgStatus.ACTIVE)
                logger.info { "Organization ${user.organizationId} successfully activated upon owner verification." }
            }.onFailure { e ->
                logger.error(e) { "Failed to activate organization ${user.organizationId}: ${e.message}" }
            }
        }

        logger.info { "User $resolvedUserId verified successfully with purpose ${command.purpose}. Issuing auth tokens." }

        sessionIssuer.createAuthenticatedSession(
            user = user,
            workspaceId = command.workspaceId,
            metadata = command.metadata
        )
    }

    data class Command(
        val purpose: OtpPurpose,
        val userId: String,
        val organizationId: String,
        val code: String,
        val workspaceId: String?,
        val metadata: SessionMetadata
    )
}
