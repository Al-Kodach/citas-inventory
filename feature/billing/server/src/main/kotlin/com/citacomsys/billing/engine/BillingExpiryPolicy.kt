package com.citacomsys.billing.engine

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

object BillingExpiryPolicy {

    val SMS_TOPUP_VALIDITY = 3.days

    val UPGRADE_VALIDITY = 7.days

    val RENEWAL_VALIDITY = 14.days
}