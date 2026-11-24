package com.citacomsys.organization.service

object BusinessCodeGenerator {

    private val normalizeRegex = Regex("[^a-z0-9]+")

    /**
     * Example:
     * "MediPlus Pharmacy Ltd" -> "MEDIplus-pharmacy-8A9F2B1C"
     */
    fun generate(
        businessName: String,
        suffixLength: Int = 8
    ): String {
        require(businessName.isNotBlank()) {
            "Business ID cannot be blank"
        }

        val slug = businessName
            .trim()
            .lowercase()
            .replace("&", "and")
            .replace(normalizeRegex, "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(32)

        val suffix = randomSuffix(suffixLength)

        return "$slug-$suffix".uppercase()
    }

    private fun randomSuffix(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        return buildString {
            repeat(length) {
                append(chars.random())
            }
        }
    }
}
