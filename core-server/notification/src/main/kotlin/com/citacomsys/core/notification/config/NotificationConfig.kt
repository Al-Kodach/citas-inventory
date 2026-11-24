package com.citacomsys.core.notification.config

import com.typesafe.config.Config

internal data class NotificationConfig(
    val maxRetries: Int,
    val baseDelayMillis: Long,
    val maxDelayMillis: Long,
    val jitterMinFactor: Double,
    val jitterMaxFactor: Double
)

internal fun getConfig(config: Config): NotificationConfig {
    return NotificationConfig(
        maxRetries = config.getInt("notification.retry.maxRetries"),
        baseDelayMillis = config.getLong("notification.retry.baseDelayMillis"),
        maxDelayMillis = config.getLong("notification.retry.maxDelayMillis"),
        jitterMinFactor = config.getDouble("notification.retry.jitterMinFactor"),
        jitterMaxFactor = config.getDouble("notification.retry.jitterMaxFactor")
    )
}