package com.citacomsys.auth.infra.repository

import com.citacomsys.api.auth.UserSession
import com.citacomsys.api.auth.UserSessionRepository
import com.citacomsys.api.auth.UserSessionView
import com.citacomsys.api.auth.WorkstationSession
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.query.queryPage
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.table.user.UserSessions
import com.citacomsys.core.persistence.table.user.Users
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock
import kotlin.time.Instant

internal class ExposedUserSessionRepository : UserSessionRepository {
    private fun ResultRow.toSession(): UserSession =
        UserSession(
            id = this[UserSessions.id].toString(),
            userId = this[UserSessions.userId].toString(),
            organizationId = this[UserSessions.organizationId].toString(),
            workspaceId = this[UserSessions.workspaceId]?.toString(),
            deviceId = this[UserSessions.deviceId],
            createdAt = this[UserSessions.createdAt],
            lastUsedAt = this[UserSessions.lastUsedAt],
            revoked = this[UserSessions.isRevoked],
            revokedAt = this[UserSessions.revokedAt],
            revokeReason = this[UserSessions.revokeReason],
            lastIp = this[UserSessions.lastIp] ?: "0.0.0.0",
            userAgent = this[UserSessions.userAgent],
            platform = this[UserSessions.platform],
            appVersion = this[UserSessions.appVersion]
        )

    private fun ResultRow.toSessionView(): UserSessionView =
        UserSessionView(
            id = this[UserSessions.id].toString(),
            userId = this[UserSessions.userId].toString(),
            userName = this[Users.name],
            userEmail = this[Users.email],
            organizationId = this[UserSessions.organizationId].toString(),
            workspaceId = this[UserSessions.workspaceId]?.toString(),
            workspaceName = this.getOrNull(Workspaces.name),
            deviceId = this[UserSessions.deviceId],
            createdAt = this[UserSessions.createdAt],
            lastUsedAt = this[UserSessions.lastUsedAt],
            revoked = this[UserSessions.isRevoked],
            lastIp = this[UserSessions.lastIp] ?: "0.0.0.0",
            userAgent = this[UserSessions.userAgent],
            platform = this[UserSessions.platform],
            appVersion = this[UserSessions.appVersion]
        )

    private fun ResultRow.toDeviceSession() = WorkstationSession(
        id = this[UserSessions.id].toString(),
        userId = this[UserSessions.userId].toString(),
        organizationId = this[UserSessions.organizationId].toString(),
        workspaceId = this[UserSessions.workspaceId]?.toString(),
        deviceId = this[UserSessions.deviceId],
        createdAt = this[UserSessions.createdAt],
        lastUsedAt = this[UserSessions.lastUsedAt],
        isActive = !this[UserSessions.isRevoked]
    )

    override suspend fun getDeviceSessions(
        organizationId: String,
        deviceId: String
    ): List<WorkstationSession> {
        return dbQuery {
            UserSessions.selectAll()
                .where {
                    UserSessions.deviceId eq deviceId and
                            (UserSessions.organizationId eq organizationId.toUuid())
                }
                .map { it.toDeviceSession() }
        }
    }

    override suspend fun fetch(
        query: PageQuery,
        organizationId: String
    ): PageResult<UserSessionView> {
        val join = UserSessions.innerJoin(Users, { userId }, { id })
            .leftJoin(Workspaces, { UserSessions.workspaceId }, { id })

        return join.queryPage(
            pageQuery = query,
            searchColumns = listOf(Users.name, Users.username, Users.email, UserSessions.lastIp),
            filterBuilder = {
                andWhere { UserSessions.organizationId eq organizationId.toUuid() }
            },
            defaultSort = UserSessions.lastUsedAt,
            mapper = { it.toSessionView() }
        )
    }

    override suspend fun save(session: UserSession): Unit = dbQuery {
        UserSessions.upsert {
            it[id] = session.id.toUuid()
            it[userId] = session.userId.toUuid()
            it[organizationId] = session.organizationId.toUuid()
            it[workspaceId] = session.workspaceId?.toUuid()
            it[deviceId] = session.deviceId
            it[createdAt] = session.createdAt
            it[lastUsedAt] = session.lastUsedAt
            it[isRevoked] = session.revoked
            it[revokedAt] = session.revokedAt
            it[revokeReason] = session.revokeReason
            it[lastIp] = session.lastIp
            it[userAgent] = session.userAgent
            it[platform] = session.platform
            it[appVersion] = session.appVersion
        }
    }

    override suspend fun find(sessionId: String): UserSession? = dbQuery {
        UserSessions.selectAll()
            .where { UserSessions.id eq sessionId.toUuid() }
            .singleOrNull()?.toSession()
    }

    override suspend fun updateLastUsed(sessionId: String, ipAddress: String, at: Instant): Unit =
        dbQuery {
            UserSessions.update({ UserSessions.id eq sessionId.toUuid() }) {
                it[lastUsedAt] = at
                it[this.lastIp] = ipAddress
            }
        }

    override suspend fun updateWorkspace(sessionId: String, workspaceId: String) = dbQuery {
        UserSessions.update({ UserSessions.id eq sessionId.toUuid() }) {
            it[this.workspaceId] = workspaceId.toUuid()
            it[lastUsedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun revoke(sessionId: String) {
        dbQuery {
            UserSessions.update({ UserSessions.id eq sessionId.toUuid() }) {
                it[isRevoked] = true
                it[revokedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun revokeByDevice(deviceId: String) {
        dbQuery {
            UserSessions.update({ UserSessions.deviceId eq deviceId }) {
                it[isRevoked] = true
                it[revokedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun revokeByUser(userId: String) {
        dbQuery {
            UserSessions.update({ UserSessions.userId eq userId.toUuid() }) {
                it[isRevoked] = true
                it[revokedAt] = Clock.System.now()
            }
        }
    }
}
