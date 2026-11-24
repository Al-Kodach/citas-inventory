package com.citacomsys.backup.fileUtil

import com.citacomsys.backup.api.fileUtil.BackupCryptoService

import okio.Path
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

internal class BackupCryptoServiceImpl : BackupCryptoService {

    private val saltBytes = "CitaPharmacySalt".encodeToByteArray() // 16 bytes for derivation
    private val iterationCount = 10_000
    private val keyLength = 256

    override suspend fun encrypt(inputPath: Path, outputPath: Path, password: String) {
        val rawBytes = File(inputPath.toString()).readBytes()

        // 1. Derive AES key from password
        val secretKey = deriveKey(password)

        // 2. Generate random Initialization Vector (IV) for GCM
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }

        // 3. Configure Cipher
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv) // 128-bit authentication tag
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        // 4. Encrypt
        val encryptedBytes = cipher.doFinal(rawBytes)

        // 5. Build output payload: write IV size (1 byte), IV bytes, and then encrypted payload
        val outputFile = File(outputPath.toString())
        outputFile.outputStream().use { stream ->
            stream.write(iv.size)
            stream.write(iv)
            stream.write(encryptedBytes)
        }
    }

    override suspend fun decrypt(inputPath: Path, outputPath: Path, password: String) {
        val inputFile = File(inputPath.toString())

        inputFile.inputStream().use { stream ->
            // 1. Extract IV parameters
            val ivSize = stream.read()
            val iv = ByteArray(ivSize).apply { stream.read(this) }

            // 2. Extract remaining encrypted payload payload
            val encryptedBytes = stream.readBytes()

            // 3. Derive key and initialize Cipher
            val secretKey = deriveKey(password)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            // 4. Decrypt and write back out to staging
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            File(outputPath.toString()).writeBytes(decryptedBytes)
        }
    }

    override suspend fun checksum(path: Path): String {
        val fileBytes = File(path.toString()).readBytes()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(fileBytes)

        // Return hash as standard clean hex string
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(password: String): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, iterationCount, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }
}
