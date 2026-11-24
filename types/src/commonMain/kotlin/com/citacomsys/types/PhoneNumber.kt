package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline


@Serializable
@JvmInline
@SerialName("PhoneNumber")
value class PhoneNumber(val value: String) {

    /**
     * Returns the E.164 format (e.g., +23359xxxxxxx).
     * This is the version we use for database lookups and authentication.
     */
    val normalized: String
        get() = PhoneNumberFormatter.formatE164(value)

    /**
     * Returns a human-readable national format (e.g., 059 xxx xxxx)
     */
    val national: String
        get() = PhoneNumberFormatter.formatNational(value)

    val mask: String
        get() {
            val clean = value.filter { it.isDigit() }
            if (clean.length <= 6) return "*** ** **"
            val first4 = clean.take(4)
            val last2 = clean.takeLast(2)
            val stars = "*".repeat(clean.length - 6)
            return "$first4$stars$last2"
        }

    companion object {
        fun tryCreate(value: String): PhoneNumber? {
            return try {
                create(value)
            } catch (e: Exception) {
                null
            }
        }

        fun create(raw: String): PhoneNumber {
            require(PhoneNumberFormatter.isValid(raw)) { "Invalid phone number: $raw" }
            // We store the E.164 version as the internal value for consistency
            return PhoneNumber(PhoneNumberFormatter.formatE164(raw))
        }
    }
}

// The Bridge
internal expect object PhoneNumberFormatter {
    fun formatE164(raw: String): String
    fun formatNational(raw: String): String
    fun isValid(raw: String): Boolean
}

