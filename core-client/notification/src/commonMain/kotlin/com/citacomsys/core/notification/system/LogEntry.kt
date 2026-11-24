package com.citacomsys.core.notification.system

import io.github.aakira.napier.LogLevel
import kotlinx.serialization.Serializable

@Serializable
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String?,
    val message: String
)
