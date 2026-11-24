package com.citacomsys.api.auth

interface RefreshTokenRepository {

    suspend fun save(record: RefreshTokenRecord)

    suspend fun find(tokenId: String): RefreshTokenRecord?

    suspend fun revokeAll(userId: String)

    suspend fun revoke(tokenId: String)

    suspend fun revokeFamily(familyId: String)
}