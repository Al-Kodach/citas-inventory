package com.citacomsys.types.jvm

import com.citacomsys.types.Money
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.plus
import kotlin.text.toDouble

private val GHANA_LOCALE = Locale("en", "GH")
const val MONEY_SCALE = 100

@Serializable(with = JvmMoneySerializer::class)
data class JvmMoney(
    val amount: BigDecimal,
    val currency: Currency = DEFAULT_CURRENCY
) {

    fun toCommon(): Money {
        val minor = amount
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        return Money(minor, currency.currencyCode)
    }

    private val currencyFormatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(GHANA_LOCALE).apply {
            this.currency = this@JvmMoney.currency
        }
    }

    operator fun plus(other: JvmMoney): JvmMoney {
        require(currency == other.currency) {
            "Cannot add Money with different currencies: $currency and ${other.currency}"
        }
        return JvmMoney(amount + other.amount, currency)
    }

    operator fun times(quantity: Int): JvmMoney {
        return JvmMoney(amount.multiply(BigDecimal(quantity)), currency)
    }

    operator fun minus(other: JvmMoney): JvmMoney {
        require(currency == other.currency) {
            "Cannot subtract Money with different currencies: $currency and ${other.currency}"
        }
        return JvmMoney(amount - other.amount, currency)
    }

    fun divide(divisor: Int): JvmMoney {
        return JvmMoney(amount.divide(BigDecimal(divisor)), currency)
    }

    operator fun unaryMinus(): JvmMoney = JvmMoney(amount.negate(), currency)

    fun formatted(): String {
        val formatter = NumberFormat.getCurrencyInstance(GHANA_LOCALE)
        formatter.currency = currency
        return formatter.format(amount)
    }

    /**
     * Formats the amount as a currency string, including the currency symbol (e.g., "GHS 1,500.00").
     */
    val formattedWithSymbol: String
        get() = currencyFormatter.format(amount)

    fun isPositive() = amount > BigDecimal.ZERO
    fun isNegative() = amount < BigDecimal.ZERO
    fun isZero() = amount.compareTo(BigDecimal.ZERO) == 0
    fun percentage(value: Int) =
        JvmMoney(amount.multiply(BigDecimal(value)).divide(BigDecimal(100)), currency)

    companion object {
        val DEFAULT_CURRENCY: Currency = Currency.getInstance("GHS")

        fun zero(currency: Currency = DEFAULT_CURRENCY): JvmMoney =
            JvmMoney(BigDecimal.ZERO, currency)

        fun fromDb(
            amount: BigDecimal,
            currencyCode: String = DEFAULT_CURRENCY.currencyCode
        ): Money {
            return JvmMoney(amount, Currency.getInstance(currencyCode)).toCommon()
        }

        fun fromCommon(money: Money): JvmMoney {
            val major = BigDecimal(money.amountMinor)
                .divide(BigDecimal(MONEY_SCALE))

            return JvmMoney(
                major,
                Currency.getInstance(money.currencyCode)
            )
        }
    }
}

fun Money.toJvm(): JvmMoney = JvmMoney.fromCommon(this)

/**
 * Custom KSerializer for the Money class.
 *
 * This serializer converts a Money object into a simple JSON object
 * containing a 'amount' (as a Double) and 'currency' (as a String).
 * It safely handles the conversion to and from BigDecimal/Currency.
 */
object JvmMoneySerializer : KSerializer<JvmMoney> {

    // A temporary, private class to define the JSON structure we want.
    @Serializable
    private data class MoneySurrogate(val amount: Double, val currency: String)

    // Describe the structure we are creating, based on our surrogate class.
    override val descriptor: SerialDescriptor = MoneySurrogate.serializer().descriptor

    /**
     * Called when SAVING a Money object to JSON.
     */
    override fun serialize(encoder: Encoder, value: JvmMoney) {
        // Create an instance of our simple surrogate class.
        val surrogate = MoneySurrogate(
            amount = value.amount.toDouble(),
            currency = value.currency.currencyCode
        )
        // Encode the surrogate, which will produce the clean JSON.
        encoder.encodeSerializableValue(MoneySurrogate.serializer(), surrogate)
    }

    /**
     * Called when LOADING a Money object from JSON.
     */
    override fun deserialize(decoder: Decoder): JvmMoney {
        // Decode the incoming JSON into our simple surrogate class.
        val surrogate = decoder.decodeSerializableValue(MoneySurrogate.serializer())
        // Construct the real Money object, safely converting the types.
        return JvmMoney(
            amount = BigDecimal.valueOf(surrogate.amount),
            currency = Currency.getInstance(surrogate.currency)
        )
    }
}
