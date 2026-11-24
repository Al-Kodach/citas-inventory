package com.citacomsys.types

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlinx.serialization.Contextual

@Serializable
data class TopProductMetric(
    val productId: String,
    val productName: String,
    val totalRevenueMinor: Long,
    val unitsSold: Long,
    val growthPercentage: Double = 0.0
)

@Serializable
data class TrendPoint(
    val label: String, // e.g., "Mon", "Jul 21"
    val value: Double,
    @Contextual val timestamp: Instant
)
