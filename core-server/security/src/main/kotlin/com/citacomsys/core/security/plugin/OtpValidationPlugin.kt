package com.citacomsys.core.security.plugin

import com.citacomsys.core.security.mfa.OtpPurpose
import com.citacomsys.core.security.mfa.OtpValidationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.request.header
import io.ktor.server.response.respond
import org.koin.ktor.ext.inject

class RequireOtpConfig {
    lateinit var purpose: OtpPurpose
}

val RequireOtp = createRouteScopedPlugin(
    name = "RequireOtp",
    createConfiguration = ::RequireOtpConfig
) {

    on(CallSetup) { call ->

        val otpService: OtpValidationService by call.application.inject()

        val code = call.request.header("X-OTP-Code")
            ?: return@on call.respond(
                HttpStatusCode.BadRequest,
                "Missing OTP code"
            )

        val principal = call.userPrincipal()

        val result = otpService.verify(
            userId = principal.userId,
            purpose = pluginConfig.purpose,
            code = code
        )

        if (result.isFailure) {
            return@on call.respond(
                HttpStatusCode.Forbidden,
                "Invalid OTP code"
            )
        }
    }
}
