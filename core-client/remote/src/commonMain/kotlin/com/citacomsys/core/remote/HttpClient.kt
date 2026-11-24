package com.citacomsys.core.remote

import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.remote.plugin.ReadyGate
import com.citacomsys.core.remote.plugin.ACTInjectionPlugin
import com.citacomsys.core.remote.plugin.OtpCodeInjectionPlugin
import com.citacomsys.core.remote.plugin.WorkstationSignaturePlugin
import com.citacomsys.core.security.crypto.SignatureProvider
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.mesh.gateway.LocalMeshRelayPlugin
import com.citacomsys.core.mesh.gateway.installMeshRetryPolicy
import com.citacomsys.core.mesh.security.installMeshLanTls
import com.citacomsys.device.service.DeviceService
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.DeviceFingerprintMismatch
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.AuthTokens
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.util.AttributeKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val SkipClientDefaultsKey = AttributeKey<Boolean>("SkipClientDefaults")

private val refreshMutex = Mutex()

internal fun createHttpClient(
    tokenStore: TokenStore,
    deviceService: DeviceService,
    signatureProvider: SignatureProvider,
    actionConfirmationManager: ActionConfirmationManager,
    sessionManager: AppSessionManager,
    syncPrefs: SyncPreferencesRepository? = null,
    platformStore: PlatformRuntimeStore? = null,
    authClient: HttpClient
): HttpClient {
    return HttpClient {
        expectSuccess = false

        syncPrefs?.let {
            installMeshLanTls(syncPrefs)
        }

        install(HttpTimeout) {
            socketTimeoutMillis = 120_000
        }

        install(SSE)

        defaultRequest {
            url(BuildConfig.BASE_URL)
            contentType(ContentType.Application.Json)
            header("X-App-Flavor", sessionManager.activeProductType.name)

            if (!headers.contains("X-Branch-Id")) {
                sessionManager.writeTargetBranchId.value?.let { header("X-Branch-Id", it) }
            }

            if (!headers.contains("X-View-Branch-Id")) {
                sessionManager.activeViewBranchId.value?.let { header("X-View-Branch-Id", it) }
            }

            if (!headers.contains("X-Workspace-Id")) {
                sessionManager.activeWorkspaceId?.let { header("X-Workspace-Id", it) }
            }
        }

        install(ReadyGate) {
            this.tokenStore = tokenStore
        }

        install(ACTInjectionPlugin) {
            this.actionTokenProvider = actionConfirmationManager
        }

        install(OtpCodeInjectionPlugin) {
            this.actionConfirmationManager = actionConfirmationManager
        }

        install(LocalMeshRelayPlugin) {
            this.preferences = syncPrefs
        }

        install(HttpRequestRetry) {
            retryIf(maxRetries = 3) { _, response ->
                response.status == HttpStatusCode.ServiceUnavailable ||
                response.status == HttpStatusCode.BadGateway ||
                response.status == HttpStatusCode.GatewayTimeout
            }
            syncPrefs?.let {
                installMeshRetryPolicy(syncPrefs)
            }
        }

        install(WorkstationSignaturePlugin) {
            this.signatureProvider = signatureProvider
            this.deviceService = deviceService
            this.sessionManager = sessionManager
            this.platformStore = platformStore
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.v(tag = "HttpClient", message = message)
                }
            }
            level = LogLevel.ALL // Dev mode
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = tokenStore.getAccessToken() ?: return@loadTokens null
                    val refreshToken = tokenStore.getRefreshToken()

                    BearerTokens(
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    )
                }

                refreshTokens {
                    val oldAccessToken = tokenStore.getAccessToken()

                    refreshMutex.withLock {
                        val latestAccessToken = tokenStore.getAccessToken()
                        val latestRefreshToken = tokenStore.getRefreshToken()

                        if (latestAccessToken != oldAccessToken && latestAccessToken != null) {
                            Napier.i { "[Auth] Token already refreshed by another request. Reusing." }
                            return@refreshTokens BearerTokens(
                                latestAccessToken,
                                latestRefreshToken ?: ""
                            )
                        }

                        Napier.i { "[Auth] Refresh triggered. Performing network call." }
                        val refreshToken = tokenStore.getRefreshToken()

                        if (refreshToken == null) {
                            Napier.e { "Refresh token not found. Clearing user session." }
                            tokenStore.clearUserSession()
                            EventBus.publish(AppLifecycleEvent.UserSessionRevoked)
                            return@refreshTokens null
                        }

                        try {
                            val response = authClient.post("auth/refresh") {
                                header("Authorization", "Bearer $refreshToken")
                                header("X-Refresh-Token", refreshToken)
                            }

                            if (response.status.isSuccess()) {
                                val tokens = response.body<AuthTokens>()
                                tokenStore.saveAccessToken(tokens.accessToken)
                                tokenStore.saveRefreshToken(tokens.refreshToken)

                                BearerTokens(tokens.accessToken, tokens.refreshToken)
                            } else {
                                val errorText = runCatching { response.bodyAsText() }.getOrDefault("")
                                Napier.e { "[Auth] Refresh failed with status ${response.status}: $errorText" }

                                handleUnauthenticatedError(
                                    statusCode = response.status,
                                    errorText = errorText,
                                    platformStore = platformStore,
                                    tokenStore = tokenStore
                                )

                                null
                            }
                        } catch (e: Exception) {
                            Napier.e(e) { "Auth refresh failed" }
                            handleRefreshFailure(e, platformStore, tokenStore)
                            if (e is DeviceFingerprintMismatchException) {
                                EventBus.publish(DeviceFingerprintMismatch)
                            }
                            null
                        }
                    }
                }

                sendWithoutRequest { request ->
                    val path = request.url.encodedPath

                    !SecurityRegistry.isPublicRoute(path)
                            && !SecurityRegistry.isActionRequiredRoute(path)
                }
            }
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    val errorText = runCatching { response.bodyAsText() }.getOrDefault("").trim()
                    Napier.w { "[Auth] Received 401 response from path: ${response.request.url.encodedPath}" }
                    handleUnauthenticatedError(
                        statusCode = response.status,
                        errorText = errorText,
                        platformStore = platformStore,
                        tokenStore = tokenStore
                    )
                }
            }
        }
    }
}


/**
 * Handles unauthenticated 401 errors:
 * - If error is a workspace/organization identity error: clears PlatformRuntimeStoreImpl
 * - Else: clears user session and notifies subscribers
 */
private suspend fun handleUnauthenticatedError(
    statusCode: HttpStatusCode,
    errorText: String,
    platformStore: PlatformRuntimeStore?,
    tokenStore: TokenStore
) {
    if (statusCode == HttpStatusCode.Unauthorized) {
        handleUnauthenticated401(errorText, platformStore, tokenStore)
    }
}

private suspend fun handleUnauthenticatedError(
    exception: Throwable,
    platformStore: PlatformRuntimeStore? = null,
    tokenStore: TokenStore? = null
) {
    if (exception is ApiUnauthorizedException || exception is DeviceFingerprintMismatchException) {
        handleUnauthenticated401(exception.message, platformStore, tokenStore)
    }
}

/**
 * Handles the logic for mapping refresh errors to logouts or store clearing
 */
private suspend fun handleRefreshFailure(
    e: Exception,
    platformStore: PlatformRuntimeStore?,
    tokenStore: TokenStore
) {
    Napier.e(e) { "[Auth] Token refresh failed with exception." }

    val (statusCode, errorText) = when (e) {
        is ClientRequestException -> e.response.status to runCatching { e.response.bodyAsText() }.getOrDefault(e.message)
        is ApiException -> e.httpStatusCode to e.message
        else -> null to (e.message ?: "")
    }

    if (statusCode == HttpStatusCode.Unauthorized) {
        handleUnauthenticated401(errorText, platformStore, tokenStore)
    } else {
        val isAuthError = e is ApiUnauthorizedException ||
                (e is ClientRequestException && e.response.status == HttpStatusCode.Unauthorized)

        if (isAuthError && e !is ApiThrottlingException) {
            handleUnauthenticated401(errorText, platformStore, tokenStore)
        }
    }
}

internal fun createAuthClient(
    tokenStore: TokenStore,
    deviceService: DeviceService,
    signatureProvider: SignatureProvider,
    sessionManager: AppSessionManager,
    platformStore: PlatformRuntimeStore? = null
) = HttpClient {
    expectSuccess = false

    install(ReadyGate) {
        this.tokenStore = tokenStore
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        })
    }


    defaultRequest {
        url(BuildConfig.BASE_URL)
        contentType(ContentType.Application.Json)
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Napier.v(tag = "HttpClient", message = message)
            }
        }
        level = LogLevel.ALL // Dev mode
    }

    install(WorkstationSignaturePlugin) {
        this.signatureProvider = signatureProvider
        this.deviceService = deviceService
        this.sessionManager = sessionManager
        this.platformStore = platformStore
    }

    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            val exception = when (cause) {
                is ClientRequestException -> {
                    val response = cause.response
                    val errorText = response.bodyAsText().trim()
                    mapToApiException(
                        errorText = errorText,
                        response = response,
                        metadata = HttpErrorMetadata(
                            method = response.request.method,
                            url = response.request.url,
                            status = response.status,
                            requestId = null
                        )
                    )
                }
                else -> cause
            }

            handleUnauthenticatedError(exception, platformStore, tokenStore)

            if (exception is Exception) {
                throw exception
            }
        }
    }
}
