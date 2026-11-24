package com.citacomsys.api.auth.service

import com.citacomsys.api.auth.SessionMetadata
import com.citacomsys.api.auth.UserSessionView
import com.citacomsys.api.auth.WorkstationSession
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.User

interface SessionService {
    suspend fun getDeviceSessions(organizationId: String, deviceId: String): List<WorkstationSession>

    suspend fun fetch(query: PageQuery, organizationId: String): PageResult<UserSessionView>

    suspend fun create(
        user: User,
        workspaceId: String?,
        metadata: SessionMetadata,
    ): AuthTokens

    suspend fun revokeSession(
        sessionId: String,
        userId: String
    )

    suspend fun revokeAllSessions(
        userId: String,
        reason: String,
        deviceId: String
    )

    suspend fun revokeDevice(deviceId: String)

    suspend fun updateWorkspace(
        sessionId: String,
        workspaceId: String
    )
}
