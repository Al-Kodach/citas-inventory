package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
enum class BillingStatus {
    /**
     * User has initiated the request in-app but hasn't provided proof
     * or the admin hasn't checked the MoMo statement yet.
     */
    PENDING,

    /**
     * Payment submitted, waiting for admin to verify.
     */
    PAYMENT_SUBMITTED,

    /**
     * Admin has confirmed the cash/MoMo received matches the Reference Code.
     */
    PAID,


    /**
     * Business effect applied
     */
    FULFILLED,


    /**
     * Admin found a discrepancy (wrong amount) or the reference was invalid.
     */
    REJECTED,

    /**
     * Payment was verified but subsequently reversed (rare for manual, but good for safety)
     */
    REFUNDED,

    /**
     * Payment valid but fulfillment failed
     */
    FULFILMENT_FAILED,

    /**
     * User cancelled the request before paying.
     */
    CANCELED,

    /**
     * Payment expired.
     */
    EXPIRED;

    fun isTerminal(): Boolean = when (this) {
        REJECTED, CANCELED, EXPIRED -> true
        else -> false
    }
}
