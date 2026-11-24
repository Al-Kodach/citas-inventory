package com.citacomsys.core.remote.plugin

import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.remote.PublicEndpointKey
import com.citacomsys.core.remote.SecurityRegistry
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * A Ktor HttpClient plugin that pauses outgoing requests until the
 * required tokens (Device or User) are available in the TokenStore.
 *
 * Logic:
 * 1. Public Paths: No waiting (Onboarding, Recovery).
 * 2. Device-Protected: Waits for Device Token (Login, Refresh, Runtime, Sync).
 * 3. User-Protected: Waits for Access Token (standard application data).
 */
class ReadyGate(
    private val tokenStore: TokenStore
) {

    class Config {
        lateinit var tokenStore: TokenStore
    }

    companion object Plugin : HttpClientPlugin<Config, ReadyGate> {
        override val key: AttributeKey<ReadyGate> = AttributeKey("ReadyGate")

        override fun prepare(block: Config.() -> Unit): ReadyGate {
            val config = Config().apply(block)
            return ReadyGate(
                tokenStore = config.tokenStore,
            )
        }

        override fun install(plugin: ReadyGate, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { context ->
                val path = context.url.encodedPath

                val isPublic = context.attributes.getOrNull(PublicEndpointKey) == true
                        || SecurityRegistry.isPublicRoute(path)

                if (!isPublic) {
                    Napier.d(tag = "ReadyGate") { "Waiting for Access Token for path: $path" }
                    plugin.tokenStore.accessToken.filterNotNull().first()
                } else {
                    Napier.d(tag = "ReadyGate") { "Allowing public path: $path" }
                }
                
                execute(context)
            }
        }
    }
}
