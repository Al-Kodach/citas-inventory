package com.citacomsys.core.security.crypto

import android.annotation.SuppressLint
import android.util.Base64
import com.citacomsys.citread.security.wipe
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * SECURITY COMPLIANCE MODULE: Cryptographic Services (Android Implementation)
 * Framework Alignment: NIST SP 800-53 Rev 5
 */
actual object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    actual fun encrypt(data: ByteArray, secretKey: ByteArray): String {
        val keyBytes = masterKeyBytes(secretKey)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(data)

        val combined = iv + ciphertext
        keyBytes.wipe() // Explicit wipe of temporary key buffer

        return Base64Helper.encode(combined)
    }

    actual fun decrypt(encryptedData: String, secretKey: ByteArray): ByteArray {
        val keyBytes = masterKeyBytes(secretKey)
        val combined = Base64Helper.decode(encryptedData)

        require(combined.size > IV_LENGTH_BYTE) { "Invalid encrypted payload" }

        val iv = combined.copyOfRange(0, IV_LENGTH_BYTE)
        val ciphertext = combined.copyOfRange(IV_LENGTH_BYTE, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val result = cipher.doFinal(ciphertext)

        keyBytes.wipe()
        return result
    }

    actual fun generateRandomKey(): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes
    }

    actual fun hmac(data: ByteArray, secretKey: ByteArray): ByteArray {
        val keyBytes = masterKeyBytes(secretKey)
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(keyBytes, HMAC_ALGORITHM))

        val result = mac.doFinal(data)
        keyBytes.wipe()
        return result
    }

    actual fun sha256(data: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data.toByteArray(Charsets.UTF_8))
    }

    actual fun sha256(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data)
    }

    actual fun deriveKeys(masterKey: ByteArray, dnaSecret: ByteArray?): DerivedKeys {
        val ikm = masterKeyBytes(masterKey)
        val salt = dnaSecret?.let { masterKeyBytes(it) } ?: ByteArray(32)
        val prk = hkdfMac(salt, ikm)

        // HKDF-Expand: Generate non-overlapping keys for encryption and signing
        val encKey = hkdfMac(prk, "citread-encryption-v1".toByteArray() + 0x01.toByte())
        val signKey = hkdfMac(prk, "citread-signing-v1".toByteArray() + 0x01.toByte())

        ikm.wipe()
        salt.wipe()
        prk.wipe()

        return DerivedKeys(encryptionKey = encKey, signingKey = signKey)
    }

    actual fun deriveDatabaseKey(masterKey: ByteArray): ByteArray {
        val ikm = masterKeyBytes(masterKey)
        val salt = "citread-db-salt-v1".toByteArray()
        val prk = hkdfMac(salt, ikm)
        val dbKey = hkdfMac(prk, "citread-database-encryption-v1".toByteArray() + 0x01.toByte())

        ikm.wipe()
        prk.wipe()

        return dbKey
    }

    private fun hkdfMac(key: ByteArray, data: ByteArray): ByteArray {
        val hmac = Mac.getInstance(HMAC_ALGORITHM)
        hmac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return hmac.doFinal(data)
    }

    actual fun generateRequestId(): String = UUID.randomUUID().toString()
    actual fun generateUUID(): String = UUID.randomUUID().toString()
}

actual object Base64Helper {
    @SuppressLint("NewApi")
    actual fun encode(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    @SuppressLint("NewApi")
    actual fun decode(base64: String): ByteArray {
        return try {
            Base64.decode(base64, Base64.NO_WRAP)
        } catch (e: Exception) {
            java.util.Base64.getDecoder().decode(base64)
        }
    }
}
