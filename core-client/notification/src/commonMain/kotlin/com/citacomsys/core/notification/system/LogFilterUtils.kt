package com.citacomsys.core.notification.system

import io.github.aakira.napier.LogLevel

object LogFilterUtils {
    fun filterLogs(
        logs: List<LogEntry>,
        level: LogLevel? = null,
        query: String = ""
    ): List<LogEntry> {
        return logs.filter { entry ->
            val matchesLevel = level == null || entry.level == level
            val matchesQuery = query.isBlank() || 
                entry.message.contains(query, ignoreCase = true) ||
                (entry.tag?.contains(query, ignoreCase = true) == true)
            matchesLevel && matchesQuery
        }
    }
}
