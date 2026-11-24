package com.citacomsys.module.inventory.ui.screen

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

object BatchGenerator {
    fun generateSystemBatch(category: String, now: Instant = Clock.System.now()): String {
        val prefix = category.take(3).uppercase()
        val timestamp = now.toEpochMilliseconds().toString().takeLast(6)
        val randomSuffix = (100..999).random()
        return "SYS-$prefix-$timestamp-$randomSuffix"
    }
}


object PharmacyExpiryRules {
    fun calculateDefaultExpiry(
        category: String,
        now: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): LocalDate {
        return when (category.uppercase()) {
            "ANTIBIOTICS" -> now.plus(12, DateTimeUnit.MONTH) // Antibiotics expire quickly
            "CARDIOVASCULAR", "ANTI-DIABETICS" -> now.plus(
                24,
                DateTimeUnit.MONTH
            ) // Chronic meds have long shelf lives
            "SUPPLEMENTS" -> now.plus(36, DateTimeUnit.MONTH) // Vitamins keep well
            else -> now.plus(18, DateTimeUnit.MONTH) // Default baseline safety buffer
        }
    }
}

object MarginCalculator {
    fun calculateMargin(costMinorStr: String, sellingMinorStr: String): String {
        val cost = costMinorStr.toLongOrNull() ?: 0L
        val selling = sellingMinorStr.toLongOrNull() ?: 0L

        if (selling <= 0L) return "0.0%"
        if (selling < cost) return "Negative Margin"

        // Calculate percentage: ((Selling - Cost) * 1000) / Selling
        // Multiplying by 1000 provides 1 decimal digit of precision using pure integer math
        val profit = selling - cost
        val rawPercentageTimesTen = (profit * 1000L) / selling

        // Extract the major and minor parts manually to avoid Java's String.format()
        val major = rawPercentageTimesTen / 10L
        val minor = rawPercentageTimesTen % 10L

        return "$major.$minor%"
    }
}
