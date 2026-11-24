package com.citacomsys.auth.infra.repository

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.api.auth.RefreshTokenRecord
import com.citacomsys.core.persistence.table.user.UserSessions
import com.citacomsys.api.auth.RefreshTokenRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

internal class ExposedRefreshTokenRepository : RefreshTokenRepository {

    override suspend fun save(record: RefreshTokenRecord) {
        UserSessions.update({ UserSessions.id eq record.sessionId.toUuid() }) {
            it[tokenHash] = record.tokenHash
            it[familyId] = record.familyId.toUuid()
            it[expiresAt] = record.expiresAt
            it[isRevoked] = false
        }
    }

    override suspend fun find(tokenId: String): RefreshTokenRecord? {
        return UserSessions
            .selectAll()
            .where { UserSessions.id eq tokenId.toUuid() }
            .singleOrNull()
            ?.let {
                RefreshTokenRecord(
                    tokenId = it[UserSessions.id].toString(),
                    userId = it[UserSessions.userId].toString(),
                    tokenHash = it[UserSessions.tokenHash],
                    expiresAt = it[UserSessions.expiresAt],
                    revoked = it[UserSessions.isRevoked],
                    familyId = it[UserSessions.familyId].toString(),
                    sessionId = it[UserSessions.id].toString()
                )
            }
    }

    override suspend fun revoke(tokenId: String) {
        UserSessions.update({ UserSessions.id eq tokenId.toUuid() }) {
            it[isRevoked] = true
        }
    }

    override suspend fun revokeFamily(familyId: String) {
        UserSessions.update({
            (UserSessions.familyId eq familyId.toUuid())
        }) {
            it[isRevoked] = true
        }
    }

    override suspend fun revokeAll(userId: String) {
        UserSessions.update(
            { UserSessions.userId eq userId.toUuid() }
        ) {
            it[isRevoked] = true
        }
    }

    fun ResultRow.toRecord(): RefreshTokenRecord {
        return RefreshTokenRecord(
            tokenId = this[UserSessions.id].toString(),
            userId = this[UserSessions.userId].toString(),
            tokenHash = this[UserSessions.tokenHash],
            expiresAt = this[UserSessions.expiresAt],
            revoked = this[UserSessions.isRevoked],
            familyId = this[UserSessions.familyId].toString(),
            sessionId = this[UserSessions.id].toString()
        )
    }
}
