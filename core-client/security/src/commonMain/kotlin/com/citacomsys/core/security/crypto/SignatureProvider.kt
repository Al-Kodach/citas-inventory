package com.citacomsys.core.security.crypto

/**
 * Provides cryptographic identity for a workstation.
 * Uses asymmetric signing to prove hardware ownership.
 */
interface SignatureProvider {
    /**
     * Returns the persistent public key for this workstation (Base64).
     * Generates a new key pair if none exists.
     */
    suspend fun getWorkstationPublicKey(): String

    /**
     * Signs the provided data using the workstation's private key.
     * Returns the signature as a Base64 string.
     */
    suspend fun sign(data: ByteArray): String

    /**
     * Signs a standard workstation verification payload.
     * Payload: "$activeHardwareId:$activeWorkspaceId:$timestamp:$nonce"
     */
    suspend fun signWorkstationClaim(
        hardwareId: String,
        workspaceId: String,
        timestamp: Long,
        nonce: String
    ): String {
        val payload = "$hardwareId:$workspaceId:$timestamp:$nonce"
        return sign(payload.encodeToByteArray())
    }
}
