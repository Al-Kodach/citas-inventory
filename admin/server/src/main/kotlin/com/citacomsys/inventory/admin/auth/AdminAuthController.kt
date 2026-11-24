package com.citacomsys.inventory.admin.auth

import com.citacomsys.api.auth.SessionMetadata
import com.citacomsys.auth.usecase.RefreshUserTokenUseCase
import com.citacomsys.core.persistence.transaction.writeTransaction
import com.citacomsys.core.security.plugin.userPrincipal
import com.citacomsys.shared.error.AuthenticationException
import com.citacomsys.types.BindDeviceRequest
import com.citacomsys.types.LoginRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.requireHeader
import io.ktor.server.response.respond

internal class AdminAuthController(
    private val adminLoginService: AdminLoginService,
    private val refreshTokenUseCase: RefreshUserTokenUseCase
) {

    suspend fun login(call: ApplicationCall) {
        val request = call.receive<LoginRequest>()
        val hardwareId = call.requireHeader("X-Hardware-Id")
        val publicKey = call.request.header("X-Workstation-Puk")
        val fingerprint = call.request.header("X-Hardware-Fingerprint")

        val sessionMetadata = SessionMetadata(
            ipAddress = call.request.origin.remoteAddress,
            deviceId = hardwareId,
            userAgent = call.request.header("User-Agent") ?: "UNKNOWN",
            publicKey = publicKey,
            fingerprint = fingerprint
        )

        val result = call.writeTransaction {
            adminLoginService.authenticate(
                identity = request.identity,
                password = request.password,
                currentDeviceId = hardwareId,
                sessionMetadata = sessionMetadata
            )
        }

        call.respond(result)
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

    suspend fun bindDevice(call: ApplicationCall) {
        val request = call.receive<BindDeviceRequest>()
        val requestIp = call.request.origin.remoteAddress

        val response = call.writeTransaction {
            adminLoginService.bindDevice(
                identifier = request.identifier,
                deviceId = request.deviceId,
                otpCode = request.otpCode ?: error("Missing OTP code"),
                requestIp = requestIp
            )
        }

        call.respond(response)
    }

    suspend fun requestMigration(call: ApplicationCall) {
        val request = call.receive<Map<String, String>>()
        val identity = request["identifier"] ?: error("Missing identifier")
        val password = request["password"] ?: error("Missing password")

        call.writeTransaction {
            adminLoginService.requestDeviceMigration(identity, password).getOrThrow()
        }

        call.respond(HttpStatusCode.OK)
    }

    suspend fun confirmMigration(call: ApplicationCall) {
        val request = call.receive<Map<String, String>>()
        val identity = request["identifier"] ?: error("Missing identifier")
        val otpCode = request["otp"] ?: error("Missing OTP code")
        val newDeviceId = request["hardwareId"] ?: error("Missing hardwareId")

        call.writeTransaction {
            adminLoginService.confirmDeviceMigration(identity, otpCode, newDeviceId).getOrThrow()
        }

        call.respond(HttpStatusCode.OK)
    }

    suspend fun logout(call: ApplicationCall) {
        call.respond(Unit)
    }

    suspend fun generateAct(call: ApplicationCall) {
        call.respond(mapOf("token" to "admin-action-token"))
    }

    suspend fun resetPasswordRequest(call: ApplicationCall) {
        call.respond(HttpStatusCode.OK)
    }

    suspend fun resetPasswordSubmit(call: ApplicationCall) {
        call.respond(HttpStatusCode.OK)
    }

    suspend fun verifyMfa(call: ApplicationCall) {
        val otpCode = call.request.header("X-OTP-Code") ?: error("Missing OTP code")
        val id = call.request.header("X-User-Id") ?: call.request.header("X-Identity") ?: error("Missing User ID / Identity")
        val hardwareId = call.requireHeader("X-Hardware-Id")
        val publicKey = call.request.header("X-Workstation-Puk")
        val fingerprint = call.request.header("X-Hardware-Fingerprint")

        val sessionMetadata = SessionMetadata(
            ipAddress = call.request.origin.remoteAddress,
            deviceId = hardwareId,
            userAgent = call.request.header("User-Agent") ?: "UNKNOWN",
            publicKey = publicKey,
            fingerprint = fingerprint
        )

        val session = call.writeTransaction {
            adminLoginService.verifyMfa(
                id = id,
                otpCode = otpCode,
                currentDeviceId = hardwareId,
                sessionMetadata = sessionMetadata
            )
        }

        call.respond(session)
    }
}
