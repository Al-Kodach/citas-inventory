package com.citacomsys.core.security

import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.shared.AppConfig
import com.citacomsys.types.AuthTokens
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow

internal class TokenStoreImpl(
    private val vault: KSafe,
    appConfig: AppConfig,
    scope: CoroutineScope
) : TokenStore {
    private val accessTokenKey = "${appConfig.prefsPrefix}_access_token"
    private val refreshTokenKey = "${appConfig.prefsPrefix}_refresh_token"

    private val _accessToken by vault.asMutableStateFlow<String?>(null, scope, accessTokenKey)
    override val accessToken = _accessToken.asStateFlow()

    private val _refreshToken by vault.asMutableStateFlow<String?>(null, scope, refreshTokenKey)
    override val refreshToken = _refreshToken.asStateFlow()


    override suspend fun saveAccessToken(accessToken: String) {
        vault.put(accessTokenKey, accessToken)
    }

    override suspend fun saveRefreshToken(refreshToken: String) {
        vault.put(refreshTokenKey, refreshToken)
    }

    override suspend fun getAccessToken(): String? {
        return vault.get(accessTokenKey, null)
    }

    override suspend fun getRefreshToken(): String? {
        return vault.get(refreshTokenKey, null)
    }

    override suspend fun clearUserSession() {
        vault.delete(accessTokenKey)
        vault.delete(refreshTokenKey)
    }

    override suspend fun clearAll() {
        vault.delete(accessTokenKey)
        vault.delete(refreshTokenKey)
    }

    override suspend fun save(authTokens: AuthTokens) {
        vault.put(accessTokenKey, authTokens.accessToken)
        vault.put(refreshTokenKey, authTokens.refreshToken)
    }
}