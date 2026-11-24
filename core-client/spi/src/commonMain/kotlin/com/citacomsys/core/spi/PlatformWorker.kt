package com.citacomsys.core.spi

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

enum class NetworkConstraint {
    NONE, CONNECTED, UNMETERED
}

data class WorkerConstraints(
    val network: NetworkConstraint = NetworkConstraint.CONNECTED,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
)

sealed interface WorkerPolicy {
    data class OneTime(
        val initialDelay: Duration = Duration.ZERO,
    ) : WorkerPolicy

    data class Periodic(
        val interval: Duration = 1.hours,
        val flexInterval: Duration? = null,
    ) : WorkerPolicy
}

enum class WorkerResult {
    SUCCESS, RETRY, FAILURE
}

enum class WorkerScope {
    GLOBAL,
    WORKSPACE_SCOPED
}

/**
 * Foreground notification metadata displayed when a [PlatformWorker] runs as a foreground service.
 */
data class ForegroundNotificationInfo(
    val notificationId: Int = 1001,
    val channelId: String = "platform_background_sync",
    val channelName: String = "Platform Background Sync",
    val title: String,
    val message: String,
)

/**
 * Plugin interface for background platform tasks.
 * Modules register instances of [PlatformWorker] in Koin, and [PlatformWorkManagerEngine]
 * automatically collects, manages workspace scopes, and schedules them natively.
 */
interface PlatformWorker {
    val workerId: String
    val workerScope: WorkerScope get() = WorkerScope.WORKSPACE_SCOPED
    val policy: WorkerPolicy
    val constraints: WorkerConstraints get() = WorkerConstraints()
    val foregroundInfo: ForegroundNotificationInfo? get() = null

    suspend fun doWork(workspaceId: String? = null): WorkerResult
}
