package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("email")
data class EmailAddress(
    val email: String? = null
) {

    /**
     * Masks the email address if present.
     */
    fun getMaskedEmail(): String? {
        val currentEmail = email ?: return null
        if (!currentEmail.contains("@")) return currentEmail

        val parts = currentEmail.split("@")
        val username = parts[0]
        val domain = parts[1]

        val maskedUsername = when {
            username.length <= 2 -> {
                username.first() + "*".repeat(username.length - 1)
            }
            else -> {
                val middleMask = "*".repeat(username.length - 2)
                "${username.first()}$middleMask${username.last()}"
            }
        }

        return "$maskedUsername@$domain"
    }
}
