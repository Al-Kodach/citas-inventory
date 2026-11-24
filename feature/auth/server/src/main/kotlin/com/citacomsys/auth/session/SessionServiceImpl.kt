package com.citacomsys.auth.session

import com.citacomsys.api.auth.*
import com.citacomsys.api.auth.service.SessionService
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.SecurityActionEvent
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.notification.service.SseService
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.AuthorizationContextProvider
import com.citacomsys.core.security.hashing.HashUtils
import com.citacomsys.core.security.service.UserTokenIssuer
import com.citacomsys.core.security.token.jwt.TokenType
import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.inventory.api.user.PlatformAdminRepository
import com.citacomsys.shared.error.AuthenticationException
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.EntityType
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.User
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update

import java.util.UUID
import kotlin.time.Clock

internal class SessionServiceImpl(
    private val userSessionRepository: UserSessionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userTokenIssuer: UserTokenIssuer,
    private val userRepository: UserPort,
    private val adminUserRepository: PlatformAdminRepository,
    private val auditLogger: TelemetryAuditor,
    private val sseService: SseService,
    private val authorizationContextProvider: AuthorizationContextProvider
) : RefreshTokenService, SessionService {

    private val logger = KotlinLogging.logger { }

    override suspend fun getDeviceSessions(organizationId: String, deviceId: String): List<WorkstationSession> {
        return userSessionRepository.getDeviceSessions(organizationId, deviceId)
    }

    override suspend fun fetch(
        query: PageQuery,
        organizationId: String
    ): PageResult<UserSessionView> {
        return userSessionRepository.fetch(query, organizationId)
    }

    override suspend fun create(
        user: User,
        workspaceId: String?,
        metadata: SessionMetadata,
    ): AuthTokens {

        val sessionId = UUID.randomUUID().toString()

        val session = UserSession(
            id = sessionId,
            userId = user.id,
            organizationId = user.organizationId,
            workspaceId = workspaceId,
            deviceId = metadata.deviceId,
            lastIp = metadata.ipAddress,
            userAgent = metadata.userAgent,

            revoked = false,
            createdAt = Clock.System.now(),
            lastUsedAt = Clock.System.now(),
            revokedAt = Clock.System.now(),
            revokeReason = null,
            platform = null,
            appVersion = "1.0.0"
        )

        userSessionRepository.save(session)

        // Capture / Upsert node standing in HardwareRegistry during authentication & session creation
        if (metadata.deviceId.isNotBlank()) {
            runCatching {
                dbQuery {
                    val updatedCount =
                        HardwareRegistry.update({ HardwareRegistry.hardwareId eq metadata.deviceId }) {
                            it[organizationId] = user.organizationId.toUuid()
                            it[masterStatus] = HardwareMasterStatus.ACTIVE
                            if (!metadata.fingerprint.isNullOrBlank()) {
                                it[fingerprint] = metadata.fingerprint
                            }
                            if (!metadata.publicKey.isNullOrBlank()) {
                                it[publicKey] = metadata.publicKey
                            }
                            it[updatedAt] = Clock.System.now()
                        }

                    if (updatedCount == 0) {
                        HardwareRegistry.insert {
                            it[hardwareId] = metadata.deviceId
                            it[organizationId] = user.organizationId.toUuid()
                            it[masterStatus] = HardwareMasterStatus.ACTIVE
                            if (!metadata.fingerprint.isNullOrBlank()) {
                                it[fingerprint] = metadata.fingerprint
                            }
                            it[createdAt] = Clock.System.now()
                            it[updatedAt] = Clock.System.now()
                        }
                    }
                }
                authorizationContextProvider.invalidateDevice(metadata.deviceId)
                logger.info { "Node device ${metadata.deviceId} captured/registered for organization ${user.organizationId} (Fingerprint: ${metadata.fingerprint ?: "N/A"})" }
            }.onFailure { e ->
                logger.warn(e) { "Failed to capture/upsert hardware registry for device ${metadata.deviceId}: ${e.message}" }
            }
        }

        val accessToken = userTokenIssuer.issueAccess(
            userId = user.id,
            organizationId = user.organizationId,
            workspaceId = workspaceId,
            username = user.username,
            role = user.role,
            sessionId = session.id,
            deviceId = metadata.deviceId
        )

        val refresh = userTokenIssuer.issueRefresh(
            userId = user.id,
            organizationId = user.organizationId,
            workspaceId = workspaceId,
            role = user.role,
            sessionId = session.id,
            deviceId = session.deviceId,
            username = user.username
        )

        refreshTokenRepository.save(
            RefreshTokenRecord(
                tokenId = refresh.tokenId.toString(),
                familyId = UUID.randomUUID().toString(),
                sessionId = session.id,
                userId = user.id,
                tokenHash = HashUtils.sha256(refresh.token),
                expiresAt = refresh.expiresAt,
                revoked = false
            )
        )

        auditLogger.logIdentityAccessEvent(
            userId = user.id,
            action = "SESSION_CREATED",
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refresh.token
        )
    }

    override suspend fun refresh(
        principal: UserPrincipal,
        rawRefreshToken: String,
        requestIp: String,
    ): AuthTokens {

        try {
            require(principal.tokenType == TokenType.REFRESH) {
                "Invalid token type"
            }

            require(principal.tokenId != null) {
                "Missing token id"
            }

            val tokenId = principal.tokenId!!
            val sessionId = principal.sessionId

            val incomingHash = HashUtils.sha256(rawRefreshToken)

            val record = refreshTokenRepository.find(tokenId)
                ?: throw AuthenticationException("Refresh token not found")

            validateRefreshRecord(
                record = record,
                incomingHash = incomingHash,
                principal = principal
            )

            val session = userSessionRepository.find(sessionId)
                ?: throw AuthenticationException("Session not found")

            validateSession(
                session = session,
                principal = principal
            )

            val user = userRepository.findById(principal.userId).getOrNull() ?: run {
                adminUserRepository.findById(principal.userId).getOrNull()
                    ?.toUser()
                    ?: throw AuthenticationException("User not found")
            }

            refreshTokenRepository.revoke(tokenId)

            val accessToken = userTokenIssuer.issueAccess(
                userId = user.id,
                organizationId = user.organizationId,
                username = user.username,
                role = user.role,
                sessionId = session.id,
                deviceId = session.deviceId,
                workspaceId = session.workspaceId
            )

            val refresh = userTokenIssuer.issueRefresh(
                userId = user.id,
                organizationId = user.organizationId,
                role = user.role,
                sessionId = session.id,
                deviceId = session.deviceId,
                username = user.username,
                workspaceId = session.workspaceId
            )

            refreshTokenRepository.save(
                RefreshTokenRecord(
                    tokenId = refresh.tokenId.toString(),
                    familyId = record.familyId,
                    sessionId = session.id,
                    userId = user.id,
                    tokenHash = HashUtils.sha256(refresh.token),
                    expiresAt = refresh.expiresAt,
                    revoked = false
                )
            )

            userSessionRepository.updateLastUsed(
                session.id,
                requestIp,
                Clock.System.now()
            )

            auditLogger.logIdentityAccessEvent(
                userId = user.id,
                action = "REFRESH_TOKEN",
                outcome = "SUCCESS",
                level = TelemetryLevel.INFO
            )

            return AuthTokens(
                accessToken = accessToken,
                refreshToken = refresh.token
            )
        } catch (e: Exception) {
            logger.error(e) { "Refresh token failed" }
            auditLogger.logIdentityAccessEvent(
                userId = principal.userId,
                action = "REFRESH_TOKEN",
                outcome = "FAILURE",
                failureReason = e.message ?: "Unknown error",
            )
            throw AuthenticationException(e.message ?: "Unknown error: Refresh token failed")
        }
    }

    override suspend fun revokeSession(
        sessionId: String,
        userId: String
    ) {
        val session = userSessionRepository.find(sessionId)
        userSessionRepository.revoke(sessionId)

        session?.deviceId?.let {
            sseService.disconnect(it)
        }

        auditLogger.logIdentityAccessEvent(
            userId = userId,
            action = SecurityActionEvent.USER_SESSION_REVOKED.name,
            outcome = "SUCCESS",
            level = TelemetryLevel.INFO
        )

        auditLogger.logApplicationEvent(
            action = "SESSION_REVOKED",
            entityType = EntityType.USER.name,
            entityId = userId,
            summary = "User session $sessionId was revoked",
            level = TelemetryLevel.INFO
        )
    }

    override suspend fun revokeAllSessions(
        userId: String,
        reason: String,
        deviceId: String
    ) {
        userSessionRepository.revokeByUser(userId)
        sseService.disconnect(deviceId)
    }

    override suspend fun revokeDevice(deviceId: String) {
        userSessionRepository.revokeByDevice(deviceId)
        sseService.disconnect(deviceId)

        auditLogger.logApplicationEvent(
            action = "DEVICE_SESSION_PURGE",
            entityType = EntityType.DEVICE.name,
            entityId = deviceId,
            summary = "All sessions for device $deviceId were purged",
            level = TelemetryLevel.WARNING
        )
    }

    override suspend fun updateWorkspace(sessionId: String, workspaceId: String) {
        userSessionRepository.updateWorkspace(sessionId, workspaceId)
    }

    private fun validateSession(
        session: UserSession,
        principal: UserPrincipal,
    ) {

        if (session.revoked) {
            throw AuthenticationException("Session revoked")
        }

        if (session.userId != principal.userId) {
            throw AuthenticationException("Session mismatch")
        }

        if (session.id != principal.sessionId) {
            throw AuthenticationException("Invalid session")
        }
    }

    private suspend fun validateRefreshRecord(
        record: RefreshTokenRecord,
        incomingHash: String,
        principal: UserPrincipal,
    ) {

        if (record.revoked) {
            logger.error { "Refresh token revoked, revoking family. Replay suspected for device: ${principal.deviceId}" }

            auditLogger.logIdentityAccessEvent(
                action = "REFRESH_TOKEN",
                outcome = "FAILURE",
                failureReason = SecurityActionEvent.REFRESH_REPLAY_DETECTED.name,
                level = TelemetryLevel.CRITICAL
            )

            refreshTokenRepository.revokeFamily(record.familyId)
            userSessionRepository.revoke(record.sessionId)
            sseService.disconnect(principal.deviceId)

            throw AuthenticationException("Session terminated due to security violation")
        }

        if (record.tokenHash != incomingHash) {
            logger.error { "Refresh token tempered, revoking family" }

            auditLogger.logIdentityAccessEvent(
                action = "REFRESH_TOKEN",
                outcome = "FAILURE",
                failureReason = SecurityActionEvent.REFRESH_TAMPER_DETECTED.name,
                level = TelemetryLevel.CRITICAL
            )

            refreshTokenRepository.revokeFamily(record.familyId)

            throw AuthenticationException("Refresh token integrity failure")
        }

        if (record.expiresAt < Clock.System.now()) {
            logger.error { "Token expired, revoking family" }

            refreshTokenRepository.revokeFamily(record.familyId)

            throw AuthenticationException("Refresh token expired")
        }
    }
}
