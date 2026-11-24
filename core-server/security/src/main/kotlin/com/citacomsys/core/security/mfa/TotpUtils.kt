package com.citacomsys.core.security.mfa

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import kotlin.experimental.and

object TotpUtils {
    private const val TIME_STEP_SECONDS = 30L
    private const val CODE_DIGITS = 6

    fun generateSecret(): ByteArray {
        val bytes = ByteArray(20) // 160 bits
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    fun encodeBase32(bytes: ByteArray): String {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val builder = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                builder.append(base32Chars[(buffer shr (bitsLeft - 5)) and 0x1F])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            builder.append(base32Chars[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return builder.toString()
    }

    fun decodeBase32(base32: String): ByteArray {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleanInput = base32.uppercase().replace("=", "")
        var buffer = 0
        var bitsLeft = 0
        val out = mutableListOf<Byte>()
        for (c in cleanInput) {
            val valIndex = base32Chars.indexOf(c)
            if (valIndex < 0) continue
            buffer = (buffer shl 5) or valIndex
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }

    fun generateTotp(secretBase32: String, timeMillis: Long = System.currentTimeMillis()): String {
        val key = decodeBase32(secretBase32)
        val time = timeMillis / 1000L / TIME_STEP_SECONDS
        val data = ByteArray(8)
        var t = time
        for (i in 7 downTo 0) {
            data[i] = (t and 0xFF).toByte()
            t = t ushr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)

        val offset = (hash[hash.size - 1] and 0x0F).toInt()
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % 1000000
        return otp.toString().padStart(CODE_DIGITS, '0')
    }

    fun verifyTotp(secretBase32: String, code: String, timeMillis: Long = System.currentTimeMillis(), window: Int = 1): Boolean {
        val currentEpoch = timeMillis / 1000L / TIME_STEP_SECONDS
        for (i in -window..window) {
            val checkTime = (currentEpoch + i) * TIME_STEP_SECONDS * 1000L
            if (generateTotp(secretBase32, checkTime) == code) {
                return true
            }
        }
        return false
    }

    fun getOtpAuthUri(accountName: String, secretBase32: String, issuer: String = "Citas"): String {
        return "otpauth://totp/$issuer:$accountName?secret=$secretBase32&issuer=$issuer"
    }
}
