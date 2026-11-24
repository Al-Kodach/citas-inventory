package com.citacomsys.core.sync

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal data class CircuitState(
    var failureCount: Int = 0,
    var lastFailureTime: Long = 0L,
    var openUntil: Long = 0L
) {
    fun isOpen(clock: Clock = Clock.System): Boolean {
        return clock.now().toEpochMilliseconds() < openUntil
    }

    fun recordFailure(policy: CircuitBreakerPolicy, clock: Clock = Clock.System) {
        val now = clock.now().toEpochMilliseconds()
        failureCount++
        lastFailureTime = now
        
        if (failureCount >= policy.failureThreshold) {
            openUntil = now + policy.cooldownMillis
        }
    }

    fun recordSuccess() {
        failureCount = 0
        openUntil = 0L
    }
}
