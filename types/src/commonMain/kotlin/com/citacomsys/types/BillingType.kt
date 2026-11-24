package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
enum class BillingType {
    /**
     * Moving from one package tier to another (e.g. Starter)
     * or increasing resource limits (Users/Storage).
     */
    SUBSCRIPTION_UPGRADE,

    /**
     * Paying to extend the current subscription for another billing cycle.
     */
    SUBSCRIPTION_RENEWAL,

    TRIAL_CONVERSION,

    /**
     * Purchasing a one-time bundle of SMS credits.
     */
    SMS_TOPUP,
}

enum class BillingOrigin {
    USER_REQUEST,
    SYSTEM_GENERATED,
    ADMIN_CREATED
}

enum class BillingReason {
    TRIAL_CONVERSION,
    PACKAGE_UPGRADE,
    SUBSCRIPTION_RENEWAL,
    SMS_TOPUP,
    MANUAL_ADJUSTMENT
}