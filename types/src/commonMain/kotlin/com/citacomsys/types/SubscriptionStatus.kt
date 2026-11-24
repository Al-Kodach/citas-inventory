package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("SubscriptionStatus")
@Serializable
enum class SubscriptionStatus {
    ACTIVE,
    TRIAL,
    GRACE_PERIOD,

    EXPIRED,
    CANCELED,
    SUSPEND;

    val isRestricted get() = this in setOf(CANCELED, SUSPEND)
}

@SerialName("SubscriptionAccess")
@Serializable
enum class SubscriptionAccess {
    FULL,
    READ_ONLY,
    BLOCKED
}

fun SubscriptionStatus.access(): SubscriptionAccess {
    return when (this) {
        SubscriptionStatus.ACTIVE,
        SubscriptionStatus.TRIAL -> SubscriptionAccess.FULL
        SubscriptionStatus.GRACE_PERIOD -> SubscriptionAccess.READ_ONLY
        else -> SubscriptionAccess.BLOCKED
    }
}

fun SubscriptionStatus.isOperational(): Boolean {
    return this in setOf(
        SubscriptionStatus.ACTIVE,
        SubscriptionStatus.TRIAL,
        SubscriptionStatus.GRACE_PERIOD
    )
}

