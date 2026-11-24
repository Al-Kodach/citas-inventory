package com.citacomsys.types

import kotlin.text.format

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.NumberParseException

internal actual object PhoneNumberFormatter {
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private const val DEFAULT_REGION = "GH"

    actual fun formatE164(raw: String): String {
        return try {
            val parsed = phoneUtil.parse(raw, DEFAULT_REGION)
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            // Fallback to basic cleaning if parsing fails
            raw.filter { it.isDigit() || it == '+' }
        }
    }

    actual fun formatNational(raw: String): String {
        return try {
            val parsed = phoneUtil.parse(raw, DEFAULT_REGION)
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
        } catch (e: Exception) {
            raw
        }
    }

    actual fun isValid(raw: String): Boolean {
        return try {
            val parsed = phoneUtil.parse(raw, DEFAULT_REGION)
            phoneUtil.isValidNumber(parsed)
        } catch (e: NumberParseException) {
            false
        }
    }
}