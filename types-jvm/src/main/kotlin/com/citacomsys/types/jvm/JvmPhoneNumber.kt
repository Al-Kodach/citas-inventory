package com.citacomsys.types.jvm

import com.citacomsys.types.PhoneNumber
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PhoneNumberSerializer::class)
class JvmPhoneNumber private constructor(
    private val googleNumber: Phonenumber.PhoneNumber
) {

    /**
     * Returns the phone number in E.164 format (e.g., "+233241234567").
     * This format is internationally recognized and unambiguous.
     */
    val e164: String by lazy {
        phoneUtil.format(
            googleNumber,
            PhoneNumberUtil.PhoneNumberFormat.E164
        )
    }

    /**
     * Returns the phone number in a national format, suitable for dialing within its region
     * (e.g., "024 123 4567" for a Ghanaian number).
     * Includes national prefix (like '0') if applicable for the region.
     */
    val national: String by lazy {
        phoneUtil.format(
            googleNumber,
            PhoneNumberUtil.PhoneNumberFormat.NATIONAL
        )
    }

    /**
     * Gets the National Significant Number (NSN).
     * This is the part of the number that follows the country code, without any national prefix
     * (e.g., "241234567" for a Ghanaian number like +233241234567).
     */
    val nationalSignificantNumber: String by lazy {
        phoneUtil.getNationalSignificantNumber(googleNumber)
    }

    /**
     * Returns the phone number in international format (e.g., "+233 24 123 4567").
     * This format includes the country code and is spaced for readability.
     */
    val international: String by lazy {
        phoneUtil.format(
            googleNumber,
            PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
        )
    }

    /**
     * Returns the type of the phone number (e.g., MOBILE, FIXED_LINE, TOLL_FREE).
     */
    val numberType: PhoneNumberUtil.PhoneNumberType by lazy {
        phoneUtil.getNumberType(googleNumber)
    }

    override fun toString(): String = e164

    /**
     * Gets the numeric country calling code for this number (e.g., 233 for Ghana).
     */
    val countryCode: Int
        get() = googleNumber.countryCode

    /**
     * Gets the two-letter ISO 3166-1 region code (e.g., "GH" for Ghana, "US" for United States)
     * for this phone number.
     * Returns null if the region cannot be determined (e.g., for some toll-free or satellite numbers).
     */
    val region: String
        get() = phoneUtil.getRegionCodeForNumber(googleNumber)

    /**
     * Checks if the number is a valid mobile number.
     */
    val isMobile: Boolean by lazy {
        val numberType = phoneUtil.getNumberType(googleNumber)
        numberType == PhoneNumberUtil.PhoneNumberType.MOBILE ||
                numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE
    }

    /**
     * Checks if the number is a valid fixed line (landline) number.
     */
    val isFixedLine: Boolean by lazy {
        val numberType = phoneUtil.getNumberType(googleNumber)
        numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE ||
                numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE
    }

    fun toCommon(): PhoneNumber = PhoneNumber(e164)

    companion object {

        private val phoneUtil = PhoneNumberUtil.getInstance()

        private const val DEFAULT_REGION = "GH"

        fun create(
            value: String,
            region: String = DEFAULT_REGION
        ): JvmPhoneNumber {
            val parsed = try {
                phoneUtil.parse(value, region)
            } catch (e: NumberParseException) {
                throw IllegalArgumentException(
                    "Invalid phone number: $value", e
                )
            }
            require(phoneUtil.isValidNumber(parsed)) {
                "Invalid phone number: $value"
            }
            return JvmPhoneNumber(parsed)
        }

        fun fromPhoneNumberString(
            value: PhoneNumber,
            region: String = DEFAULT_REGION
        ): JvmPhoneNumber {

            val parsed = try {
                phoneUtil.parse(value.value, region)
            } catch (e: NumberParseException) {
                throw IllegalArgumentException(
                    "Invalid phone number: ${value.value}", e
                )
            }

            require(phoneUtil.isValidNumber(parsed)) {
                "Invalid phone number: ${value.value}"
            }

            return JvmPhoneNumber(parsed)
        }

        fun normalize(
            value: PhoneNumber,
            region: String = DEFAULT_REGION
        ): PhoneNumber {

            val phone = fromPhoneNumberString(value, region)

            return phone.toCommon()
        }
    }
}

fun PhoneNumber.normalize(): PhoneNumber =
    JvmPhoneNumber.normalize(this)

fun PhoneNumber.toJvm(): JvmPhoneNumber =
    JvmPhoneNumber.fromPhoneNumberString(this)

/**
 * Custom serializer for the PhoneNumber class.
 *
 * It serializes a PhoneNumber object into its canonical E.164 string representation
 * and deserializes an E.164 string back into a fully validated PhoneNumber object.
 */
object PhoneNumberSerializer : KSerializer<JvmPhoneNumber> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PhoneNumber", PrimitiveKind.STRING)

    /**
     * Called when SAVING a PhoneNumber object to JSON.
     * We take the object and write its e164Format string.
     */
    override fun serialize(encoder: Encoder, value: JvmPhoneNumber) {
        encoder.encodeString(value.e164)
    }

    /**
     * Called when LOADING a PhoneNumber object from JSON.
     * We read the string and use our trusted factory method to reconstruct the object.
     */
    override fun deserialize(decoder: Decoder): JvmPhoneNumber {
        val phoneNumberString = decoder.decodeString()
        return JvmPhoneNumber.create(phoneNumberString)
    }
}