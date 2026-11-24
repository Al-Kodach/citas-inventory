package com.citacomsys.types

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

enum class BillingCycle(
    val months: Int
) {
    MONTHLY(1),
    QUARTERLY(3),
    SEMI_ANNUAL(6),
    YEARLY(12),
    TWO_YEARLY(24),
    LIFETIME(99999999)
}


fun BillingCycle.duration(): Duration =
    when (this) {
        BillingCycle.MONTHLY -> 30.days
        BillingCycle.QUARTERLY -> 90.days
        BillingCycle.SEMI_ANNUAL -> 180.days
        BillingCycle.YEARLY -> 365.days
        BillingCycle.TWO_YEARLY -> 730.days
        BillingCycle.LIFETIME -> 99999999.days
    }