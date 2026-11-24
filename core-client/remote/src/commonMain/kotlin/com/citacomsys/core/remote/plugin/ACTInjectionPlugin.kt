package com.citacomsys.core.remote.plugin

import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.remote.SecurityRegistry
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.encodedPath

class ACTInjectConfig {
    lateinit var actionTokenProvider: ActionConfirmationManager
}

/**
 * A Ktor Client plugin that attaches the Action Confirmation Token (ACT) to sensitive requests.
 * 
 * This version uses the indexer operator on headers to ensure we OVERWRITE any existing values,
 * preventing the duplication bug (semicolon-separated tokens).
 */
val ACTInjectionPlugin = createClientPlugin(
    "ACTInjectionPlugin",
    ::ACTInjectConfig
) {
    val actManager = pluginConfig.actionTokenProvider

    onRequest { request, _ ->
        val path = request.url.encodedPath
        val token = actManager.actionToken.value
        val isActionRequired = SecurityRegistry.isActionRequiredRoute(path)

        if (isActionRequired) {
            if (token != null) {
                // Use indexer to ensure we SET the value, not APPEND
                request.headers["X-Action-Token"] = token
                Napier.d { "ACTInjectionPlugin: Attached ACT for sensitive route: $path" }
            } else {
                val errorMessage =
                    "Blocking request to sensitive route $path: No Action Confirmation Token (ACT) available."
                Napier.e { errorMessage }
                throw IllegalStateException(errorMessage)
            }
        } else if (token != null) {
            // Optional: attach for non-sensitive routes if present, ensuring it's clean
            request.headers["X-Action-Token"] = token
        }
    }
}
