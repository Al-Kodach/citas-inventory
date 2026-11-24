package com.citacomsys.billing.engine

object BillingReferenceGenerator {

    fun generate(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        return "CIT-" + (1..4)
            .map { chars.random() }
            .joinToString("")
    }
}