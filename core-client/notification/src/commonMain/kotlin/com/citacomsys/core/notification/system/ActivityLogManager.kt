package com.citacomsys.core.notification.system

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.concurrent.Volatile

data class LoggingConfig(
    val noiseTags: Set<String> = setOf(
        "Navigation", "HttpClient", "AvailabilityManager", 
        "AndroidSyncEngine", "Koin", "Database", "CustomerDatabase", "FileKit", "File system",
        "WorkManager", "Napier", "InternalAntilog", "KSafe"
    ),
    val securityTags: Set<String> = setOf("SECURITY", "HUB", "NET", "Zero-Trust", "STEEL_CURTAIN", "APP_CONFIG"),
    val maxInMemoryLogs: Int = 500
)

object ActivityLogManager {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private var logConfig = LoggingConfig()
    private var logSink: ((level: LogLevel, tag: String?, message: String) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var isLoggingInProgress = false

    fun init(
        config: LoggingConfig = LoggingConfig(),
        sink: ((level: LogLevel, tag: String?, message: String) -> Unit)? = null
    ) {
        logConfig = config
        logSink = sink
    }

    fun addLog(level: LogLevel, tag: String?, message: String) {
        if (isLoggingInProgress) return

        val isNoisyTag = tag in logConfig.noiseTags
        val isNoisyMessage = message.contains("ping", ignoreCase = true) || 
                             message.contains("pong", ignoreCase = true) ||
                             message.contains("Binding node service", ignoreCase = true) ||
                             message.contains("backstack", ignoreCase = true)
        
        val isSecurity = tag in logConfig.securityTags
        
        if (!isSecurity && (isNoisyTag || isNoisyMessage) && level.ordinal < LogLevel.WARNING.ordinal) return

        isLoggingInProgress = true

        try {
            val maskedMessage = maskSensitiveInfo(message)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val timeStr = "${now.hour.toString().padStart(2, '0')}:${
                now.minute.toString().padStart(2, '0')
            }:${now.second.toString().padStart(2, '0')}"

            val newEntry = LogEntry(timeStr, level, tag, maskedMessage)
            
            _logs.update { (it + newEntry).takeLast(logConfig.maxInMemoryLogs) }

            logSink?.let { sink ->
                scope.launch {
                    try {
                        sink(level, tag, maskedMessage)
                    } catch (_: Exception) {}
                }
            }
        } finally {
            isLoggingInProgress = false
        }
    }

    private fun maskSensitiveInfo(message: String): String {
        return message
            .replace(Regex("[a-zA-Z0-9]{40,}"), "********[SECURE]********")
            .replace(Regex("authtoken[:= ]\\s*[a-zA-Z0-9_-]+"), "authtoken: ********")
            .replace(Regex("key[:= ]\\s*[a-zA-Z0-9_-]+"), "key: ********")
    }

    fun clear() {
        _logs.value = emptyList()
    }
}

class InternalAntilog(private val minLevel: LogLevel = LogLevel.VERBOSE) : Antilog() {
    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        if (priority.ordinal < minLevel.ordinal) return

        val finalMessage = if (throwable != null) {
            "${message ?: ""} \n${throwable.message}\n${throwable.stackTraceToString()}"
        } else {
            message ?: ""
        }
        
        println("[$priority] ${tag ?: "App"}: $finalMessage")
        ActivityLogManager.addLog(priority, tag, finalMessage)
    }
}
