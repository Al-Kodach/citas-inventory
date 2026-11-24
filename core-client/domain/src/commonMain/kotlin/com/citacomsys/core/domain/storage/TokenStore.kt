package com.citacomsys.core.domain.storage

import com.citacomsys.types.AuthTokens
import kotlinx.coroutines.flow.StateFlow

interface TokenStore {
    val accessToken: StateFlow<String?>
    val refreshToken: StateFlow<String?>

    suspend fun saveAccessToken(accessToken: String)
    suspend fun saveRefreshToken(refreshToken: String)

    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?

    suspend fun clearUserSession()

    suspend fun clearAll()
    suspend fun save(authTokens: AuthTokens)
}
