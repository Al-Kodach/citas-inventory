package com.citacomsys.core.data.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.time.Clock

private fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {

        engine {
            // CIO Specific Tuning
            endpoint {
                connectTimeout = 20_000      // 20s to establish TCP
                requestTimeout = 120_000     // 120s for the full request
                socketTimeout = 120_000      // 120s for socket idle
                keepAliveTime = 30_000
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 120_000
        }

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
}

val coreModule = module {
    single<CoroutineScope> { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    single<HttpClient> { createHttpClient() }
    single<Clock> { Clock.System }
}
