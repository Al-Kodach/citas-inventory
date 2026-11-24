package com.citacomsys.core.security.crypto

import com.citacomsys.citread.security.wipe

/**
 * Normalizes a secret key into a 32-byte array.
 * If the input is a valid 64-char hex string, it's decoded directly.
 * Otherwise, it's hashed using SHA-256.
 */
fun masterKeyBytes(secretKey: String): ByteArray {
    return if (secretKey.length == 64 && secretKey.all { it in "0123456789abcdefABCDEF" }) {
        ByteArray(32) { index ->
            secretKey.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    } else {
        CryptoManager.sha256(secretKey)
    }
}

/**
 * Normalizes a ByteArray secret key into a 32-byte array.
 */
fun masterKeyBytes(secretKey: ByteArray): ByteArray {
    return if (secretKey.size == 32) {
        secretKey.copyOf()
    } else {
        CryptoManager.sha256(secretKey)
    }
}

data class DerivedKeys(
    val encryptionKey: ByteArray,
    val signingKey: ByteArray
) {
    fun wipe() {
        encryptionKey.wipe()
        signingKey.wipe()
    }
}

expect object Base64Helper {
    fun encode(bytes: ByteArray): String
    fun decode(base64: String): ByteArray
}

/**
 * SECURITY COMPLIANCE MODULE: Hardened Cryptographic Primitives
 * Framework Alignment: NIST SP 800-53 SC-13, SC-28
 *
 * This service uses ByteArrays for all sensitive inputs and outputs to enable 
 * explicit memory wiping and mitigate heap-dump vulnerabilities.
 */
expect object CryptoManager {

    fun encrypt(data: ByteArray, secretKey: ByteArray): String
    fun decrypt(encryptedData: String, secretKey: ByteArray): ByteArray
    
    fun generateRandomKey(): ByteArray
    fun hmac(data: ByteArray, secretKey: ByteArray): ByteArray
    
    fun sha256(data: String): ByteArray
    fun sha256(data: ByteArray): ByteArray
    
    /**
     * Derives separate keys for encryption and signing from a master key using HKDF-SHA256.
     * @param dnaSecret The Fleet DNA secret used to anchor the derivation to the organization.
     */
    fun deriveKeys(masterKey: ByteArray, dnaSecret: ByteArray? = null): DerivedKeys
    
    /**
     * Derives a stable key specifically for database field-level encryption.
     */
    fun deriveDatabaseKey(masterKey: ByteArray): ByteArray

    fun generateRequestId(): String
    fun generateUUID(): String
}


/**
 * Helper to convert Hex string back to ByteArray.
 */
fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Must have an even length" }
    return ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
