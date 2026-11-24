package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
enum class ActionConfirmationPurpose(val claim: String) {
    GENERAL_SUDO("general_sudo"),
    TRANSFER_STOCK("transfer_stock"),
    DELETE_RESOURCES("delete_resources"),
    VERIFY_PAYMENT("verify_payment"),
    UPDATE_SECURITY_SETTINGS("update_security"),
    MANAGE_BILLING("manage_billing"),
    EXPORT_DATA("export_data"),
    SUBSCRIPTION_CHANGE("subscription_change"),
    REJECT_PAYMENT("reject_payment"),
    CHANGE_PASSWORD("change_password"),
    ENABLE_MFA("enable_mfa"),
    DISABLE_MFA("disable_mfa");

    companion object {
        fun fromClaim(claim: String): ActionConfirmationPurpose {
            return entries.find { it.claim == claim }
                ?: throw IllegalArgumentException("Unknown claim: $claim")
        }

        fun fromString(value: String): ActionConfirmationPurpose {
            return entries.find { it.name == value }
                ?: entries.find { it.claim == value }
                ?: throw IllegalArgumentException("Unknown value: $value")
        }
    }
}