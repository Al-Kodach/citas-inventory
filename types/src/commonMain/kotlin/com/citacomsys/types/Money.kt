package com.citacomsys.types

import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue
import kotlin.math.pow

/**
 * Cross-platform Money value object.
 *
 * Amount is stored in **minor units** (e.g. cents, pesewas).
 *
 * Example:
 * GHS 12.50 → amountMinor = 1250
 */
@Serializable
data class Money(
    val amountMinor: Long,
    val currencyCode: String = "GHS",
    val fractionDigits: Int = 2
) {

    constructor(double: Double, currencyCode: String = "GHS"): this(
        amountMinor = (double * 100).toLong(),
        currencyCode = currencyCode
    )

    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) {
            "Cannot add Money with different currencies: $currencyCode vs ${other.currencyCode}"
        }
        return Money(amountMinor + other.amountMinor, currencyCode)
    }

    operator fun minus(other: Money): Money {
        require(currencyCode == other.currencyCode) {
            "Cannot subtract Money with different currencies: $currencyCode vs ${other.currencyCode}"
        }
        return Money(amountMinor - other.amountMinor, currencyCode)
    }

    operator fun unaryMinus(): Money = Money(-amountMinor, currencyCode)

    /**
     * The amount as a Double for display purposes.
     * Note: Avoid using this for financial calculations. Use `amountMinor`.
     */
    val amount: Double
        get() {
            val divisor = 10.0.pow(fractionDigits)
            return amountMinor / divisor
        }

    val formatted: String
        get() {
            val divisor = 10.0.pow(fractionDigits).toLong()
            val major = amountMinor / divisor
            val minor = (amountMinor % divisor).absoluteValue

            val majorStr = major.toString()
            val result = StringBuilder()
            var digitCount = 0

            for (i in majorStr.length - 1 downTo 0) {
                val char = majorStr[i]
                if (char == '-') {
                    result.append(char)
                    continue
                }
                if (digitCount > 0 && digitCount % 3 == 0) {
                    result.append(',')
                }
                result.append(char)
                digitCount++
            }

            val formattedMajor = result.reverse().toString()
            val formattedMinor = minor.toString().padStart(fractionDigits, '0')

            return "$currencyCode $formattedMajor.$formattedMinor"
        }

    override fun toString(): String {
        return formatted
    }


    fun percentage(value: Double): Money {
        return Money(
            ((amountMinor * value) / 100).toLong(), currencyCode
        )
    }


    /**
     * Returns decimal string (e.g. "1,250.50")
     */
    fun decimal(): String {
        val divisor = 10.0.pow(fractionDigits).toLong()
        val major = amountMinor / divisor
        val minor = (amountMinor % divisor).absoluteValue

        val majorStr = major.toString()
        val result = StringBuilder()
        var digitCount = 0

        for (i in majorStr.length - 1 downTo 0) {
            val char = majorStr[i]
            if (char == '-') {
                result.append(char)
                continue
            }
            if (digitCount > 0 && digitCount % 3 == 0) {
                result.append(',')
            }
            result.append(char)
            digitCount++
        }

        return "${result.reverse()}.${minor.toString().padStart(fractionDigits, '0')}"
    }

    operator fun times(multiplier: Int): Money {
        return Money(amountMinor * multiplier, currencyCode)
    }

    fun divide(divisor: Int): Money {
        return Money(amountMinor / divisor, currencyCode)
    }

    companion object {
        private const val CURRENCY_CODE_DEFAULT = "GHS"
        private const val FRACTION_DIGITS_DEFAULT = 2

        fun zero(currency: String = CURRENCY_CODE_DEFAULT) =
            Money(0, currency)

        fun fromMajor(amount: Double, currency: String = CURRENCY_CODE_DEFAULT): Money {
            return Money((amount * 10.0.pow(FRACTION_DIGITS_DEFAULT)).toLong(), currency)
        }

        private val PARSE_REGEX = Regex("""^(\d+)(?:\.(\d{1,2}))?$""")

        fun parse(value: String): Money? {
            val trimmedValue = value.trim()
            if (trimmedValue.isBlank()) return null

            val match = PARSE_REGEX.matchEntire(trimmedValue) ?: return null

            try {
                val majorPart = match.groupValues[1].toLong()
                val decimalPartStr = match.groupValues[2]

                val decimalPart = if (decimalPartStr.isEmpty()) {
                    0L
                } else if (decimalPartStr.length == 1) {
                    decimalPartStr.toLong() * 10
                } else {
                    decimalPartStr.toLong()
                }

                val amountMinor = (majorPart * 100) + decimalPart

                return Money(amountMinor = amountMinor)
            } catch (e: NumberFormatException) {
                return null
            }
        }

    }
}
