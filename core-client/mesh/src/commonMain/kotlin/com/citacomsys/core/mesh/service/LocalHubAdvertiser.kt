package com.citacomsys.core.mesh.service

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Simplified identity for the Mother Hub on the local network.
 */
@Serializable
data class HubIdentity(
    val hubId: String,
    val hubName: String,
    val ipAddress: String,
    val port: Int,
    val tlsFingerprint: String? = null,
    val lastSeenAt: Instant
)

/**
 * Interface for platform-specific mDNS/NSD advertisers.
 */
interface LocalHubAdvertiser {
    fun start(identity: HubIdentity, secret: ByteArray)
    fun stop()
}
