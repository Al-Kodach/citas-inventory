package com.citacomsys.core.data.worker

import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.notification.system.AppNotification
import com.citacomsys.core.notification.system.AppNotificationManager
import com.citacomsys.core.spi.NetworkConstraint
import com.citacomsys.core.spi.PlatformWorkManagerEngine
import com.citacomsys.core.spi.PlatformWorker
import com.citacomsys.core.spi.WorkerConstraints
import com.citacomsys.core.spi.WorkerPolicy
import com.citacomsys.core.spi.WorkerResult
import com.citacomsys.core.spi.WorkerScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.Koin
import kotlin.time.Duration.Companion.milliseconds

/**
 * Common cross-platform implementation of [PlatformWorkManagerEngine].
 * Collects all registered [PlatformWorker] plugins from Koin on startup during BOOTSTRAP/POST_ONBOARDING,
 * managing workspace scoping, constraint satisfaction, execution state, and exponential backoff retries.
 */
class DefaultPlatformWorkManagerEngine(
    private val connectivityService: NetworkConnectivityService,
    private val scope: CoroutineScope,
) : PlatformWorkManagerEngine {
    private val TAG = "PLATFORM_WORK_ENGINE"
    private val registeredWorkers = mutableMapOf<String, PlatformWorker>()
    private val activeWorkspaceJobs = mutableMapOf<String, Job>()

    override fun registerWorker(worker: PlatformWorker) {
        registeredWorkers[worker.workerId] = worker
        Napier.i(tag = TAG) { "Registered PlatformWorker plugin: ${worker.workerId} [Scope: ${worker.workerScope}]" }
    }

    override fun getWorker(workerId: String): PlatformWorker? = registeredWorkers[workerId]

    override fun getAllWorkers(): List<PlatformWorker> = registeredWorkers.values.toList()

    override fun triggerWorkerNow(workerId: String, workspaceId: String?) {
        val worker = registeredWorkers[workerId]
        if (worker == null) {
            Napier.w(tag = TAG) { "Worker $workerId not found in engine registry." }
            return
        }
        scope.launch {
            Napier.i(tag = TAG) { "Triggering worker $workerId immediately (Workspace: ${workspaceId ?: "GLOBAL"})..." }
            executeWorkerWithNotification(worker, workspaceId)
        }
    }

    override fun triggerAllWorkersNow(workspaceId: String?) {
        registeredWorkers.values.forEach { worker ->
            triggerWorkerNow(worker.workerId, workspaceId)
        }
    }

    override fun activateWorkspaceWorkers(workspaceId: String) {
        cancelWorkspaceWorkers(workspaceId)

        val workspaceJob = scope.launch {
            Napier.i(tag = TAG) { "Activating workspace workers for workspace $workspaceId..." }
            registeredWorkers.values
                .filter { it.workerScope == WorkerScope.WORKSPACE_SCOPED }
                .forEach { worker ->
                    scheduleWorkerLoop(worker, workspaceId)
                }
        }
        activeWorkspaceJobs[workspaceId] = workspaceJob
    }

    override fun cancelWorkspaceWorkers(workspaceId: String) {
        activeWorkspaceJobs.remove(workspaceId)?.cancel()
        Napier.i(tag = TAG) { "Cancelled active workers for workspace $workspaceId" }
    }

    override fun cancelWorker(workerId: String) {
        registeredWorkers.remove(workerId)
        Napier.i(tag = TAG) { "Cancelled worker: $workerId" }
    }

    override suspend fun run(koin: Koin) {
        Napier.i(tag = TAG) { "Initializing PlatformWorkManagerEngine..." }
        val workers = koin.getAll<PlatformWorker>()
        workers.forEach { worker ->
            registerWorker(worker)
            if (worker.workerScope == WorkerScope.GLOBAL) {
                scheduleWorkerLoop(worker, null)
            }
        }
    }

    private suspend fun areConstraintsSatisfied(constraints: WorkerConstraints): Boolean {
        if (constraints.network == NetworkConstraint.CONNECTED || constraints.network == NetworkConstraint.UNMETERED) {
            if (!connectivityService.isOnline()) {
                return false
            }
        }
        return true
    }

    private fun scheduleWorkerLoop(worker: PlatformWorker, workspaceId: String?) {
        scope.launch {
            when (val policy = worker.policy) {
                is WorkerPolicy.OneTime -> {
                    if (policy.initialDelay.inWholeMilliseconds > 0) {
                        delay(policy.initialDelay)
                    }
                    executeWorkerWithNotification(worker, workspaceId)
                }

                is WorkerPolicy.Periodic -> {
                    while (true) {
                        executeWorkerWithNotification(worker, workspaceId)
                        delay(policy.interval)
                    }
                }
            }
        }
    }

    private suspend fun executeWorkerWithNotification(worker: PlatformWorker, workspaceId: String?) {
        if (!areConstraintsSatisfied(worker.constraints)) {
            Napier.d(tag = TAG) { "Skipping worker ${worker.workerId}: Network/Device constraints not satisfied." }
            return
        }

        val info = worker.foregroundInfo
        val notificationId = "worker_${worker.workerId}"

        if (info != null) {
            AppNotificationManager.addNotification(
                AppNotification(
                    id = notificationId,
                    title = info.title,
                    message = info.message,
                    isCritical = false,
                    autoDismiss = false,
                )
            )
        }

        try {
            Napier.d(tag = TAG) { "Executing worker task: ${worker.workerId} (Workspace: ${workspaceId ?: "GLOBAL"})" }
            var attempt = 0
            var result: WorkerResult = WorkerResult.RETRY
            val maxAttempts = 3

            while (attempt < maxAttempts && result == WorkerResult.RETRY) {
                attempt++
                result = try {
                    worker.doWork(workspaceId)
                } catch (e: Exception) {
                    Napier.e(tag = TAG, throwable = e) { "Worker ${worker.workerId} failed on attempt $attempt" }
                    WorkerResult.RETRY
                }

                if (result == WorkerResult.RETRY && attempt < maxAttempts) {
                    val backoffDelay = (5000L * (1 shl (attempt - 1))).milliseconds
                    Napier.w(tag = TAG) { "Worker ${worker.workerId} requested RETRY. Backing off for ${backoffDelay.inWholeSeconds}s (Attempt $attempt/$maxAttempts)..." }
                    delay(backoffDelay)
                }
            }
        } finally {
            if (info != null) {
                AppNotificationManager.removeNotification(notificationId)
            }
        }
    }
}
