package com.citacomsys.core.security.vault

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class SecretVault(masterKey: String) {
    private val algorithm = "AES"
    private val keySpec = SecretKeySpec(masterKey.padTo(16).toByteArray(), algorithm)

    @Suppress("GetInstance")
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(plainText.toByteArray())
        return Base64.getEncoder().encodeToString(encrypted)
    }

    @Suppress("GetInstance")
    fun decrypt(encryptedText: String?): String {
        if (encryptedText.isNullOrBlank()) return ""
        return try {
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText))
            String(decrypted)
        } catch (_: Exception) {
            "DECRYPTION_ERROR"
        }
    }

    private fun String.padTo(length: Int): String {
        return if (this.length >= length) this.take(length)
        else this.padEnd(length, '0')
    }
}
