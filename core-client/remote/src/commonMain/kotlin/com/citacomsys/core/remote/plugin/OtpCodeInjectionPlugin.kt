package com.citacomsys.core.remote.plugin

import com.citacomsys.core.domain.security.ActionConfirmationManager
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header

/**
 * Configuration for the client-side OtpCodeInjectionPlugin.
 */
class OtpCodeInjectionPluginConfig {
    lateinit var actionConfirmationManager: ActionConfirmationManager
}

/**
 * A Ktor Client plugin that automatically attaches the "X-OTP-Code" header
 * to requests if an OTP code is currently held in the [ActionConfirmationManager].
 */
val OtpCodeInjectionPlugin = createClientPlugin("OtpCodeInjectionPlugin", ::OtpCodeInjectionPluginConfig) {
    val manager = pluginConfig.actionConfirmationManager

    onRequest { request, _ ->
        val code = manager.otpCode.value
        if (!code.isNullOrBlank()) {
            request.header("X-OTP-Code", code)
        }
    }
}
