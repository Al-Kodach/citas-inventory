package com.citacomsys.api.auth

import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import kotlin.time.Instant

interface DeviceSessionRepository {
    suspend fun getDeviceSessions(organizationId: String, deviceId: String): List<WorkstationSession>

}

interface UserSessionRepository: DeviceSessionRepository {
    suspend fun fetch(query: PageQuery, organizationId: String): PageResult<UserSessionView>

    suspend fun save(session: UserSession)

    suspend fun find(sessionId: String): UserSession?

    suspend fun updateLastUsed(
        sessionId: String,
        ipAddress: String,
        at: Instant
    )

    suspend fun updateWorkspace(
        sessionId: String,
        workspaceId: String
    )

    suspend fun revoke(sessionId: String)

    suspend fun revokeByDevice(deviceId: String)

    suspend fun revokeByUser(userId: String)
}
