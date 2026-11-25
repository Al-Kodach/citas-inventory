package com.citacomsys.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.jvm.JvmInline

@Serializable(with = PhoneNumberSerializer::class)
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

object PhoneNumberSerializer : KSerializer<PhoneNumber> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PhoneNumber", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PhoneNumber) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): PhoneNumber {
        val input = decoder as? JsonDecoder 
            ?: return PhoneNumber(decoder.decodeString())
        val element = input.decodeJsonElement()
        val rawStr = when (element) {
            is JsonPrimitive -> element.content
            is JsonObject -> {
                val country = element["countryCode"]?.jsonPrimitive?.content ?: ""
                val national = element["nationalNumber"]?.jsonPrimitive?.content ?: ""
                "$country$national"
            }
            else -> element.toString()
        }
        return PhoneNumber.tryCreate(rawStr) ?: PhoneNumber(rawStr)
    }
}

// The Bridge
internal expect object PhoneNumberFormatter {
    fun formatE164(raw: String): String
    fun formatNational(raw: String): String
    fun isValid(raw: String): Boolean
}
