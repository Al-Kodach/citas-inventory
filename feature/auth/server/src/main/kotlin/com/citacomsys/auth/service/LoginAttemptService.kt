package com.citacomsys.auth.service

import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import java.util.concurrent.TimeUnit

internal class LoginAttemptService {

    data class LoginAttemptKey(
        val ipAddress: String,
        val deviceId: String
    )
    private val MAX_ATTEMPTS = 3

    private val attemptsCache: LoadingCache<LoginAttemptKey, Int> =
        Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build(CacheLoader { 0 })

    fun onSuccess(key: LoginAttemptKey) {
        attemptsCache.invalidate(key)
    }

    fun onFailure(key: LoginAttemptKey) {
        attemptsCache.put(key, attemptsCache[key] + 1)
    }

    fun isBlocked(key: LoginAttemptKey): Boolean =
        attemptsCache[key] >= MAX_ATTEMPTS

    fun getRemainingAttempts(key: LoginAttemptKey): Int =
        MAX_ATTEMPTS - attemptsCache[key]
}