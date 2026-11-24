package com.citacomsys.api.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("RefreshTokenRecord")
data class RefreshTokenRecord(
    val tokenId: String,
    val familyId: String,
    val sessionId: String,
    val userId: String,
    val tokenHash: String,
    val expiresAt: Instant,
    val revoked: Boolean,
)

@Serializable
@SerialName("SessionMetadata")
data class SessionMetadata(
    val deviceId: String,
    val ipAddress: String,
    val userAgent: String? = null,
    val fingerprint: String? = null,
    val publicKey: String? = null,
)

@Serializable
@SerialName("UserSession")
data class UserSession(
    val id: String,
    val userId: String,
    val organizationId: String,
    val workspaceId: String?,
    val deviceId: String,
    val createdAt: Instant,
    val lastUsedAt: Instant,
    val revoked: Boolean,
    val revokedAt: Instant?,
    val revokeReason: String?,
    val lastIp: String,
    val userAgent: String?,
    val platform: String?,
    val appVersion: String?,
)

@Serializable
@SerialName("WorkstationSession")
data class WorkstationSession(
    val id: String,
    val userId: String,
    val organizationId: String,
    val workspaceId: String?,
    val deviceId: String,
    val createdAt: Instant,
    val lastUsedAt: Instant,
    val isActive: Boolean,
)
