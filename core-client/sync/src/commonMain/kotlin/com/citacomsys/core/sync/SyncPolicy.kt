package com.citacomsys.core.sync

import com.citacomsys.types.EntityType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class SyncSlaPolicy(
    val maxStale: Duration = 5.minutes,
    val criticalMaxStale: Duration = 2.minutes
)

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMillis: Long = 1000,
    val maxDelayMillis: Long = 10_000,
    val factor: Double = 2.0
)

data class CircuitBreakerPolicy(
    val failureThreshold: Int = 3,
    val cooldownMillis: Long = 30_000
)

object SyncSlaRegistry {
    private val policies = mapOf(
        EntityType.SALE to SyncSlaPolicy(maxStale = 1.minutes, criticalMaxStale = 30.seconds),
        EntityType.BILLING to SyncSlaPolicy(maxStale = 2.minutes, criticalMaxStale = 1.minutes),
        EntityType.STOCK_BATCH to SyncSlaPolicy(maxStale = 5.minutes, criticalMaxStale = 2.minutes),
        EntityType.PRODUCT to SyncSlaPolicy(maxStale = 30.minutes, criticalMaxStale = 10.minutes)
    )

    fun getPolicy(type: EntityType): SyncSlaPolicy = policies[type] ?: SyncSlaPolicy()
}
