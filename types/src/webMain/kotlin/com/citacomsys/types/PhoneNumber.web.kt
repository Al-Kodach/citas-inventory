package com.citacomsys.types

internal actual object PhoneNumberFormatter {
    actual fun formatE164(raw: String): String {
        val cleaned = raw.replace(Regex("[^0-9]"), "")
        return when {
            cleaned.startsWith("0") -> "+233" + cleaned.substring(1)
            cleaned.length == 9 -> "+233$cleaned"
            !raw.startsWith("+") -> "+$cleaned"
            else -> raw
        }
    }

    actual fun formatNational(raw: String): String = raw

    actual fun isValid(raw: String): Boolean {
        return raw.length >= 9 // Basic JS validation
    }
}