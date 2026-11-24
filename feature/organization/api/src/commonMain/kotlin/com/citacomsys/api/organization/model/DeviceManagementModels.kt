package com.citacomsys.api.organization.model

import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.HardwareMasterStatus
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class HardwareDeviceDto(
    val hardwareId: String,
    val organizationId: String,
    val masterStatus: HardwareMasterStatus,
    val securityReason: String? = null,
    val fingerprint: String? = null,
    val model: String? = null,
    val osName: String? = null,
    val architecture: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class WorkstationBindingDto(
    val id: String,
    val hardwareId: String,
    val workspaceId: String,
    val orgId: String,
    val displayName: String,
    val workspaceName: String,
    val status: DeviceStatus,
    val lastIp: String? = null,
    val lastUsedAt: Instant,
    val createdAt: Instant
)

@Serializable
data class UserSessionDto(
    val id: String,
    val userId: String,
    val organizationId: String,
    val workspaceId: String? = null,
    val deviceId: String,
    val createdAt: Instant,
    val lastUsedAt: Instant,
    val revoked: Boolean,
    val revokedAt: Instant? = null,
    val revokeReason: String? = null,
    val lastIp: String,
    val userAgent: String? = null,
    val platform: String? = null,
    val appVersion: String? = null
)

@Serializable
data class SessionItemDto(
    val sessionId: String,
    val username: String,
    val deviceName: String,
    val createdAt: Instant,
    val ipAddr: String,
    val platform: String?
)
