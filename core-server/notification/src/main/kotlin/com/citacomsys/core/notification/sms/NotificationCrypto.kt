package com.citacomsys.core.notification.sms

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object NotificationCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    private val secureRandom = SecureRandom()

    // Fetches your master secret key from the secure environment injection variables
    private val secretKey: SecretKeySpec by lazy {
        val keyBytes = System.getenv("NOTIFICATION_CRYPTO_KEY")?.toByteArray(Charsets.UTF_8)
            ?: error("Critical Security Exception: NOTIFICATION_CRYPTO_KEY is missing from env.")
        // Ensure the key length matches AES-256 standard boundaries (32 bytes)
        SecretKeySpec(keyBytes.copyOf(32), "AES")
    }

    data class EncryptedPayload(val cipherText: ByteArray, val iv: ByteArray)

    fun encrypt(plainText: String): EncryptedPayload {
        val iv = ByteArray(IV_LENGTH_BYTE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))
        }
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(cipherText, iv)
    }

    fun decrypt(cipherText: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))
        }
        val plainTextBytes = cipher.doFinal(cipherText)
        return String(plainTextBytes, Charsets.UTF_8)
    }
}
