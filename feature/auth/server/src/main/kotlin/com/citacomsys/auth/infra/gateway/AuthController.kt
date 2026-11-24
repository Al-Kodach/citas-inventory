package com.citacomsys.auth.infra.gateway

import com.citacomsys.core.security.plugin.userPrincipal
import com.citacomsys.core.security.plugin.HardwareAttestationKey
import com.citacomsys.types.LoginRequest
import com.citacomsys.api.auth.SessionMetadata
import com.citacomsys.api.auth.service.SessionService
import com.citacomsys.auth.service.LoginService
import com.citacomsys.auth.usecase.*
import com.citacomsys.core.persistence.transaction.writeTransaction
import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.core.security.plugin.SecurityContextKey
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.resolveHardwareId
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.shared.error.AuthenticationException
import com.citacomsys.types.ActRequest
import com.citacomsys.types.ActionConfirmationPurpose
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.PageQuery
import com.citacomsys.types.ResetPasswordSubmitRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.requireHeader
import io.ktor.server.response.respond

internal class AuthController(
    private val loginService: LoginService,
    private val refreshTokenUseCase: RefreshUserTokenUseCase,
    private val sessionService: SessionService,
    private val generateActUseCase: GenerateActUseCase,
    private val mfaVerificationUseCase: MfaVerificationUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
) {

    suspend fun logout(call: ApplicationCall) {
        val principal = call.userPrincipal()
        call.application.log.debug("Logout request received from ${principal.deviceId}")

        call.writeTransaction {
            sessionService.revokeSession(principal.sessionId, principal.userId)
        }
        call.respond(Unit)
    }

    suspend fun generateAct(call: ApplicationCall) {
        val request = call.receive<ActRequest>()
        val context = call.attributes[SecurityContextKey]

        val purpose = ActionConfirmationPurpose.fromString(request.purpose)
        val principal = call.userPrincipal()
        val workspaceId = call.resolvedWorkspaceId

        val command = GenerateActUseCase.Command(
            identity = context.actorId,
            password = request.password,
            workspaceId = workspaceId,
            sessionId = principal.sessionId,
            purpose = purpose,
        )

        val token = call.writeTransaction {
            generateActUseCase.execute(command).getOrThrow()
        }

        call.respond(token)
    }

    private suspend fun authenticate(
        identity: String,
        password: String,
        deviceId: String,
        workspaceId: String?,
        call: ApplicationCall
    ) {

        val session = SessionMetadata(
            ipAddress = call.request.origin.remoteAddress,
            deviceId = deviceId,
            userAgent = call.request.header("User-Agent") ?: "UNKNOWN",
            fingerprint = call.request.headers["X-Hardware-Fingerprint"]
        )

        val command = LoginService.Command(
            userId = identity,
            password = password,
            session = session,
            workspaceId = workspaceId,
            hardwareAttestation = call.attributes.getOrNull(HardwareAttestationKey)
        )

        val result = call.writeTransaction {
            loginService.authenticate(command).getOrThrow()
        }

        call.respond(result)
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receive<LoginRequest>()
        val hardwareId = call.requireHeader("X-Hardware-Id")
        val workspaceId = call.request.headers["X-Workspace-Id"]

        authenticate(
            identity = request.identity,
            password = request.password,
            deviceId = hardwareId,
            workspaceId = workspaceId,
            call = call
        )
    }

    suspend fun resetPasswordRequest(call: ApplicationCall) {
        val request = call.receive<Map<String, String>>()
        val identity = request["identifier"] ?: error("Missing identifier")

        val challenge = call.writeTransaction {
            resetPasswordUseCase.request(
                identifier = identity
            ).getOrThrow()
        }

        call.respond(challenge)
    }

    suspend fun resetPasswordSubmit(call: ApplicationCall) {
        val request = call.receive<ResetPasswordSubmitRequest>()

        call.writeTransaction {
            resetPasswordUseCase.submit(
                challenge = request.challenge,
                newPassword = request.newPassword,
                otpCode = request.otp
            ).getOrThrow()
        }

        call.respond(HttpStatusCode.OK)
    }

    suspend fun refresh(call: ApplicationCall) {
        val principal = call.userPrincipal()

        val refreshToken = call.request.header("X-Refresh-Token")
            ?: throw AuthenticationException("Missing refresh token")

        val authToken = call.writeTransaction {
            refreshTokenUseCase.execute(
                principal = principal,
                rawRefreshToken = refreshToken,
                requestIp = call.request.origin.remoteHost
            )
        }

        call.respond(authToken)
    }

    suspend fun otpChallenge(call: ApplicationCall) {
        val deviceId = call.resolveHardwareId()

        val code = call.request.header("X-OTP-Code")
            ?: return call.respond(
                HttpStatusCode.BadRequest,
                "Missing OTP code"
            )

        val context = call.receive<AuthChallengeContext>()
        val workspaceId = call.request.headers["X-Workspace-Id"]
        val publicKey = call.request.header("X-Workstation-Puk")
        val fingerprint = call.request.header("X-Hardware-Fingerprint")


        val metadata = SessionMetadata(
            deviceId = deviceId,
            ipAddress = call.request.origin.remoteAddress,
            userAgent = call.request.header("User-Agent"),
            fingerprint = fingerprint,
            publicKey = publicKey
        )

        val otpPurpose = runCatching { OtpPurpose.valueOf(context.otpPurpose) }.getOrNull()
            ?: return call.respond(
                HttpStatusCode.BadRequest,
                "Invalid OTP purpose"
            )

        val command = MfaVerificationUseCase.Command(
            userId = context.userId,
            organizationId = context.organizationId,
            workspaceId = workspaceId,
            purpose = otpPurpose,
            code = code,
            metadata = metadata
        )

        val tokens = call.writeTransaction {
            mfaVerificationUseCase.execute(command).getOrThrow()
        }

        call.respond(tokens)
    }

    suspend fun fetchSessions(call: ApplicationCall) {
        val query = call.receive<PageQuery>()
        val organizationId = call.resolveOrganizationId

        val result = sessionService.fetch(query, organizationId)
        call.respond(result)
    }
}
