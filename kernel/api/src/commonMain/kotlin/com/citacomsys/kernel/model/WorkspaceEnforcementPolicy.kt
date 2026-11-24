package com.citacomsys.kernel.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class WorkspaceEnforcementPolicy(
    val allowLogin: Boolean,
    val allowWrites: Boolean,
    val allowSync: Boolean,
    val allowOfflineAuth: Boolean,
    val requireOnlineValidation: Boolean,
    val forceLogout: Boolean,
    val clearOfflineProfiles: Boolean,
    val requireRecovery: Boolean,
    val forceCloudOnly: Boolean,
    val offlineRevokedUserIds: Set<String>,
    val leaseExpiresAt: Instant
) {
    companion object {
        val ALWAYS_ALLOW = WorkspaceEnforcementPolicy(
            allowLogin = true,
            allowWrites = true,
            allowSync = true,
            allowOfflineAuth = true,
            requireOnlineValidation = false,
            forceLogout = false,
            clearOfflineProfiles = false,
            requireRecovery = false,
            forceCloudOnly = false,
            offlineRevokedUserIds = emptySet(),
            leaseExpiresAt = Clock.System.now().plus(Duration.parse("365d"))
        )
    }
}
