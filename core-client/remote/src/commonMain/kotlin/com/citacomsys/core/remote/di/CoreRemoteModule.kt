package com.citacomsys.core.remote.di

import com.citacomsys.core.remote.createAuthClient
import com.citacomsys.core.remote.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module


val networkModule: Module = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    single(named("authClient")) {
        createAuthClient(
            tokenStore = get(),
            deviceService = get(),
            signatureProvider = get(),
            sessionManager = get(),
            platformStore = getOrNull()
        )
    }

    single<HttpClient> {
        createHttpClient(
            tokenStore = get(),
            deviceService = get(),
            signatureProvider = get(),
            actionConfirmationManager = get(),
            sessionManager = get(),
            syncPrefs = getOrNull(),
            platformStore = getOrNull(),
            authClient = get(named("authClient"))
        )
    }

    single<BearerAuthProvider> {
        val client = get<HttpClient>()
        client.authProvider() ?: error("BearerAuthProvider not found in HttpClient")
    }
}
