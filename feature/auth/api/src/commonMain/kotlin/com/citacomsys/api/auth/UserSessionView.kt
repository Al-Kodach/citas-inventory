package com.citacomsys.api.auth

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class UserSessionView(
    val id: String,
    val userId: String,
    val userName: String,
    val userEmail: String?,
    val organizationId: String,
    val workspaceId: String?,
    val workspaceName: String?,
    val deviceId: String,
    val createdAt: Instant,
    val lastUsedAt: Instant,
    val revoked: Boolean,
    val lastIp: String,
    val userAgent: String?,
    val platform: String?,
    val appVersion: String?,
)
