package com.citacomsys.feature.user.server

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.persistence.plugin.getChangeListParameter
import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.persistence.transaction.writeTransaction
import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.userPrincipal
import com.citacomsys.core.security.service.MfaAuthInfo
import com.citacomsys.core.security.service.MfaService
import com.citacomsys.inventory.api.user.model.*
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.ChallengeType
import com.citacomsys.types.EntityType
import com.citacomsys.types.MFAChannel
import com.citacomsys.types.PageQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import java.util.UUID

internal class UserController(
    private val service: UserPort,
    private val telemetryAuditor: TelemetryAuditor,
    private val passwordHasher: PasswordHasher,
    private val mfaService: MfaService
) {

    suspend fun changePassword(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val request = call.receive<Map<String, String>>()
        val currentPassword = request["currentPassword"] ?: error("Current password required")
        val newPassword = request["newPassword"] ?: error("New password required")

        val user = service.findById(principal.userId).getOrThrow()

        if (!passwordHasher.verify(currentPassword, user.passwordHash)) {
            telemetryAuditor.logIdentityAccessEvent(
                userId = user.id,
                action = "CHANGE_PASSWORD",
                outcome = "FAILURE",
                failureReason = "INCORRECT_CURRENT_PASSWORD",
                level = TelemetryLevel.WARNING
            )
            return call.respond(HttpStatusCode.BadRequest, "Current password is incorrect.")
        }

        if (newPassword.length < 8) {
            return call.respond(HttpStatusCode.BadRequest, "New password must be at least 8 characters long.")
        }

        val hashed = passwordHasher.hash(newPassword)
        call.transactionManager.runInTransaction {
            service.updatePassword(principal.userId, hashed)
        }

        telemetryAuditor.logApplicationEvent(
            action = "CHANGE_PASSWORD",
            entityType = EntityType.USER.name,
            entityId = principal.userId,
            summary = "User ${principal.userId} changed password successfully",
            level = TelemetryLevel.INFO
        )

        call.respond(HttpStatusCode.OK)
    }
    
    suspend fun getUserWorkspaceBindingsById(call: ApplicationCall) {
        val userId = call.parameters["userId"] ?: return call.respond(HttpStatusCode.BadRequest)
        val organizationId = call.resolveOrganizationId
        val bindings = call.transactionManager.runReadOnly {
            service.getBindingsForUserId(userId, organizationId)
        }
        call.respond(bindings)
    }

    suspend fun requestMfaEnable(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val user = service.findById(principal.userId).getOrThrow()

        val info = MfaAuthInfo(
            userId = user.id,
            username = user.username,
            email = user.email,
            phone = user.phone,
            purpose = OtpPurpose.MFA_CHALLENGE
        )

        val channel = mfaService.authAccount(info).getOrThrow()
        val accountMasked = if (channel == MFAChannel.EMAIL) {
            val parts = user.email!!.split("@")
            if (parts.size == 2 && parts[0].length > 2) {
                parts[0].take(2) + "***@" + parts[1]
            } else user.email
        } else user.phone.mask

        val challenge = AuthChallengeContext(
            challengeId = UUID.randomUUID().toString(),
            userId = user.id,
            organizationId = user.organizationId,
            action = "MFA_ENABLE",
            challengeType = ChallengeType.OTP,
            title = "Multi-Factor Authentication",
            description = "Enter the verification code sent to your registered ${channel.name.lowercase()}",
            accountMasked = accountMasked,
            otpPurpose = OtpPurpose.MFA_CHALLENGE.name,
            channel = channel
        )

        call.respond(challenge)
    }

    suspend fun confirmMfaEnable(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val request = call.receive<Map<String, String>>()
        val otp = request["otp"] ?: error("OTP code required")

        // 1. Cryptographically/OTP verify code
        mfaService.verify(principal.userId, otp, OtpPurpose.MFA_CHALLENGE).getOrThrow()

        // 2. Atomically persist MFA activation upon successful verification
        call.transactionManager.runInTransaction {
            service.updateMfaEnabled(principal.userId, true)
        }

        telemetryAuditor.logApplicationEvent(
            action = "MFA_ENABLED",
            entityType = EntityType.USER.name,
            entityId = principal.userId,
            summary = "User ${principal.userId} enabled MFA securely",
            level = TelemetryLevel.INFO
        )

        call.respond(HttpStatusCode.OK)
    }

    suspend fun requestMfaDisable(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val user = service.findById(principal.userId).getOrThrow()

        val info = MfaAuthInfo(
            userId = user.id,
            username = user.username,
            email = user.email,
            phone = user.phone,
            purpose = OtpPurpose.MFA_CHALLENGE
        )

        val channel = mfaService.authAccount(info).getOrThrow()
        val accountMasked = if (channel == MFAChannel.EMAIL) {
            val parts = user.email!!.split("@")
            if (parts.size == 2 && parts[0].length > 2) {
                parts[0].take(2) + "***@" + parts[1]
            } else user.email
        } else user.phone.mask

        val challenge = AuthChallengeContext(
            challengeId = UUID.randomUUID().toString(),
            userId = user.id,
            organizationId = user.organizationId,
            action = "MFA_DISABLE",
            challengeType = ChallengeType.OTP,
            title = "Disable Multi-Factor Authentication",
            description = "Enter the verification code sent to your registered ${channel.name.lowercase()}",
            accountMasked = accountMasked,
            otpPurpose = OtpPurpose.MFA_CHALLENGE.name,
            channel = channel
        )

        call.respond(challenge)
    }

    suspend fun disableMfa(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val request = call.receive<Map<String, String>>()
        val otp = request["otp"] ?: error("OTP code required to disable MFA")

        // Cryptographically/OTP verify code before disabling MFA
        mfaService.verify(principal.userId, otp, OtpPurpose.MFA_CHALLENGE).getOrThrow()

        call.transactionManager.runInTransaction {
            service.updateMfaEnabled(principal.userId, false)
        }
        telemetryAuditor.logApplicationEvent(
            action = "MFA_DISABLED",
            entityType = EntityType.USER.name,
            entityId = principal.userId,
            summary = "User ${principal.userId} disabled MFA after OTP verification",
            level = TelemetryLevel.WARNING
        )
        call.respond(HttpStatusCode.OK)
    }

    suspend fun save(call: ApplicationCall) {
        val request = call.receive<SaveTenantUserRequest>()
        val principal = call.userPrincipal()
        val organizationId = call.resolveOrganizationId

        val existing = service.findById(request.id).getOrNull()

        val user = call.transactionManager.runInTransaction {
            service.save(request, organizationId)
        }

        telemetryAuditor.logApplicationEvent(
            action = if (existing == null) "USER_CREATED" else "USER_UPDATED",
            entityType = EntityType.USER.name,
            entityId = user.id,
            summary = "User ${user.name} was ${if (existing == null) "created" else "updated"} by ${principal.userId}",
            level = TelemetryLevel.INFO
        )

        call.respond(if (existing == null) HttpStatusCode.Created else HttpStatusCode.OK, user)
    }

    suspend fun bindUserToWorkspace(call: ApplicationCall) {
        val request = call.receive<BindUserToWorkspaceRequest>()
        val principal = call.userPrincipal()
        val organizationId = call.resolveOrganizationId

        call.writeTransaction {
            service.bindUserToWorkspace(request, organizationId).getOrThrow()
        }

        telemetryAuditor.logApplicationEvent(
            action = "USER_WORKSPACE_BOUND",
            entityType = EntityType.USER.name,
            entityId = request.userId,
            summary = "User ${request.userId} bound to workspace ${request.workspaceId} with role ${request.role} by ${principal.userId}",
            level = TelemetryLevel.INFO
        )

        call.respond(HttpStatusCode.OK)
    }

    suspend fun unbindUserFromWorkspace(call: ApplicationCall) {
        val request = call.receive<UnbindUserFromWorkspaceRequest>()
        val principal = call.userPrincipal()
        val organizationId = call.resolveOrganizationId

        call.transactionManager.runInTransaction {
            service.unbindUserFromWorkspace(request, organizationId).getOrThrow()
        }

        telemetryAuditor.logApplicationEvent(
            action = "USER_WORKSPACE_UNBOUND",
            entityType = EntityType.USER.name,
            entityId = request.userId,
            summary = "User ${request.userId} unbound from workspace ${request.workspaceId} by ${principal.userId}",
            level = TelemetryLevel.INFO
        )

        call.respond(HttpStatusCode.OK)
    }

    suspend fun findById(call: ApplicationCall) {
        val userId = call.parameters["userId"] ?: error("Missing user ID")
        val user = call.transactionManager.runReadOnly {
            service.findById(userId).getOrThrow()
        }
        call.respond(user)
    }

    suspend fun suspendUser(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val userId = call.parameters["id"] ?: return call.respond(
            HttpStatusCode.BadRequest,
            "Missing user ID"
        )

        if (principal.userId == userId) {
            return call.respond(HttpStatusCode.BadRequest, "Cannot suspend yourself")
        }

        val request = call.receive<SuspendUserRequest>()
        val reason = request.reason

        val user = call.transactionManager.runInTransaction {
            service.suspendUser(userId, reason).getOrThrow()
        }

        telemetryAuditor.logApplicationEvent(
            action = "USER_SUSPENDED",
            entityType = EntityType.USER.name,
            entityId = userId,
            summary = "User ${user.name} suspended by ${principal.userId}. Reason: $reason",
            level = TelemetryLevel.WARNING
        )

        call.respond(user)
    }

    suspend fun activateUser(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val userId = call.parameters["id"] ?: return call.respond(
            HttpStatusCode.BadRequest,
            "Missing user ID"
        )

        val user = call.transactionManager.runInTransaction {
            service.activateUser(userId).getOrThrow()
        }

        telemetryAuditor.logApplicationEvent(
            action = "USER_ACTIVATED",
            entityType = EntityType.USER.name,
            entityId = userId,
            summary = "User ${user.name} activated by ${principal.userId}",
            level = TelemetryLevel.INFO
        )

        call.respond(user)
    }

    suspend fun delete(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val userId = call.parameters["id"] ?: return call.respond(
            HttpStatusCode.BadRequest,
            "Missing user ID"
        )

        if (principal.userId == userId) {
            return call.respond(HttpStatusCode.BadRequest, "Cannot delete yourself")
        }

        call.transactionManager.runInTransaction {
            service.delete(userId, principal.userId).getOrThrow()
        }

        telemetryAuditor.logApplicationEvent(
            action = "USER_DELETED",
            entityType = EntityType.USER.name,
            entityId = userId,
            summary = "User $userId deleted by ${principal.userId}",
            level = TelemetryLevel.WARNING
        )

        call.respond(HttpStatusCode.OK)
    }

    suspend fun findByPhone(call: ApplicationCall) {
        val phone = call.parameters["phone"]!!
        val user = call.transactionManager.runReadOnly {
            service.findByPhone(phone).getOrNull()
        }
        if (user != null) {
            call.respond(user)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }



    suspend fun getActiveSessions(call: ApplicationCall) {
        val organizationId = call.resolveOrganizationId
        val sessions = call.transactionManager.runReadOnly {
            service.getActiveSessions(organizationId)
        }
        call.respond(sessions)
    }

    suspend fun getSelfWorkspaceBindings(call: ApplicationCall) {
        val principal = call.userPrincipal()
        val organizationId = call.resolveOrganizationId
        val bindings = call.transactionManager.runReadOnly {
            service.getBindingsForUserId(principal.userId, organizationId)
        }
        call.respond(bindings)
    }

    suspend fun fetchPaged(call: ApplicationCall) {
        val query = call.receive<PageQuery>()
        val organizationId = call.resolveOrganizationId
        val result = call.transactionManager.runReadOnly {
            service.fetchPaged(query, organizationId)
        }
        call.respond(result)
    }

    suspend fun changeList(call: ApplicationCall) {
        val organizationId = call.resolveOrganizationId
        val since = call.getChangeListParameter()
        val response = call.transactionManager.runReadOnly {
            service.getChangesSince(since, organizationId)
        }
        call.respond(response)
    }
}
