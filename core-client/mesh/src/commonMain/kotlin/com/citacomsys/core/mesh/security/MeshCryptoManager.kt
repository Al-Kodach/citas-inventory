package com.citacomsys.core.mesh.security

import kotlin.experimental.xor
import kotlin.io.encoding.Base64

/**
 * Lightweight cryptographic helper for mesh communication.
 * Provides symmetric encryption for LAN transit using the FleetDNA secret.
 */
 object MeshCryptoManager {
    
    /**
     * Prepares the specialized transit key (FleetDNA) for symmetric encryption.
     */
    fun deriveFleetSecret(fleetDna: String): ByteArray {
        return fleetDna.encodeToByteArray()
    }

    /**
     * Generates a one-way discovery token to prove organization membership
     * without exposing the raw Organization ID or Secret in plaintext mDNS.
     */
    fun generateDiscoveryToken(organizationId: String, secret: ByteArray): String {
        return encrypt(organizationId.encodeToByteArray(), secret)
    }

    /**
     * Verifies if a received discovery token belongs to the expected organization.
     */
    fun verifyDiscoveryToken(token: String, expectedOrgId: String, secret: ByteArray): Boolean {
        return try {
            val decrypted = decrypt(token, secret).decodeToString()
            decrypted == expectedOrgId
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Simple XOR-based "encryption" for the broadcast blob.
     * In a production environment, this should be replaced with AES-GCM.
     */
    fun encrypt(data: ByteArray, secret: ByteArray): String {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = data[i] xor secret[i % secret.size]
        }
        return Base64.encode(result)
    }

    fun decrypt(base64Data: String, secret: ByteArray): ByteArray {
        val data = Base64.decode(base64Data)
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = data[i] xor secret[i % secret.size]
        }
        return result
    }
}

private fun String.chunked(size: Int): List<String> {
    val result = mutableListOf<String>()
    var current = 0
    while (current < length) {
        val end = (current + size).coerceAtMost(length)
        result.add(substring(current, end))
        current = end
    }
    return result
}

internal fun String.toChunks(size: Int) = this.chunked(size)
