package com.citacomsys.core.security.service

import java.security.SecureRandom

object PasswordGenerator {
    private val secureRandom = SecureRandom()
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*"
    private const val ALL_CHARS = UPPER + LOWER + DIGITS + SYMBOLS

    fun generateSecurePassword(length: Int = 12): String {
        require(length >= 8) { "Password length must be at least 8 characters" }
        val sb = StringBuilder().apply {
            append(UPPER[secureRandom.nextInt(UPPER.length)])
            append(LOWER[secureRandom.nextInt(LOWER.length)])
            append(DIGITS[secureRandom.nextInt(DIGITS.length)])
            append(SYMBOLS[secureRandom.nextInt(SYMBOLS.length)])
            repeat(length - 4) {
                append(ALL_CHARS[secureRandom.nextInt(ALL_CHARS.length)])
            }
        }
        return sb.toString().toList().shuffled(secureRandom).joinToString("")
    }
}
