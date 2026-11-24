package com.citacomsys.core.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.citacomsys.core.spi.NetworkConstraint
import com.citacomsys.core.spi.PlatformWorkManagerEngine
import com.citacomsys.core.spi.PlatformWorker
import com.citacomsys.core.spi.WorkerConstraints
import com.citacomsys.core.spi.WorkerPolicy
import com.citacomsys.core.spi.WorkerScope
import io.github.aakira.napier.Napier
import org.koin.core.Koin
import java.util.concurrent.TimeUnit

/**
 * Android-native implementation of [PlatformWorkManagerEngine].
 * Collects all registered [PlatformWorker] plugins from Koin during POST_ONBOARDING and enqueues
 * them into Android's native [WorkManager] with network, charging, and battery constraints.
 */
class AndroidPlatformWorkManagerEngine(
    private val context: Context,
) : PlatformWorkManagerEngine {
    private val TAG = "ANDROID_WORK_ENGINE"
    private val workManager by lazy { WorkManager.getInstance(context) }
    private val registeredWorkers = mutableMapOf<String, PlatformWorker>()

    override fun registerWorker(worker: PlatformWorker) {
        registeredWorkers[worker.workerId] = worker
        if (worker.workerScope == WorkerScope.GLOBAL) {
            enqueueNativeWorkManagerRequest(worker, null)
        }
    }

    override fun getWorker(workerId: String): PlatformWorker? = registeredWorkers[workerId]

    override fun getAllWorkers(): List<PlatformWorker> = registeredWorkers.values.toList()

    override fun triggerWorkerNow(workerId: String, workspaceId: String?) {
        val worker = registeredWorkers[workerId] ?: return
        val constraints = buildAndroidConstraints(worker.constraints)
        val request = OneTimeWorkRequestBuilder<AndroidPlatformWorkerBridge>()
            .setInputData(
                workDataOf(
                    AndroidPlatformWorkerBridge.KEY_WORKER_ID to workerId,
                    "KEY_WORKSPACE_ID" to (workspaceId ?: "")
                )
            )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "IMMEDIATE_${workerId}_${workspaceId ?: "GLOBAL"}",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        Napier.i(tag = TAG) { "Enqueued immediate Android WorkManager request for $workerId (Workspace: ${workspaceId ?: "GLOBAL"})" }
    }

    override fun triggerAllWorkersNow(workspaceId: String?) {
        registeredWorkers.values.forEach { worker ->
            triggerWorkerNow(worker.workerId, workspaceId)
        }
    }

    override fun activateWorkspaceWorkers(workspaceId: String) {
        registeredWorkers.values
            .filter { it.workerScope == WorkerScope.WORKSPACE_SCOPED }
            .forEach { worker ->
                enqueueNativeWorkManagerRequest(worker, workspaceId)
            }
    }

    override fun cancelWorkspaceWorkers(workspaceId: String) {
        registeredWorkers.values
            .filter { it.workerScope == WorkerScope.WORKSPACE_SCOPED }
            .forEach { worker ->
                workManager.cancelUniqueWork("PERIODIC_${worker.workerId}_$workspaceId")
                workManager.cancelUniqueWork("IMMEDIATE_${worker.workerId}_$workspaceId")
            }
        Napier.i(tag = TAG) { "Cancelled Android WorkManager requests for workspace $workspaceId" }
    }

    override fun cancelWorker(workerId: String) {
        registeredWorkers.remove(workerId)
        workManager.cancelUniqueWork("PERIODIC_${workerId}")
        workManager.cancelUniqueWork("IMMEDIATE_${workerId}")
        Napier.i(tag = TAG) { "Cancelled Android WorkManager request for $workerId" }
    }

    override suspend fun run(koin: Koin) {
        Napier.i(tag = TAG) { "Initializing Android WorkManager Engine..." }
        val workers = koin.getAll<PlatformWorker>()
        workers.forEach { worker ->
            registerWorker(worker)
        }
    }

    private fun enqueueNativeWorkManagerRequest(worker: PlatformWorker, workspaceId: String?) {
        val constraints = buildAndroidConstraints(worker.constraints)
        val inputData = workDataOf(
            AndroidPlatformWorkerBridge.KEY_WORKER_ID to worker.workerId,
            "KEY_WORKSPACE_ID" to (workspaceId ?: "")
        )
        val uniqueTag = "${worker.workerId}_${workspaceId ?: "GLOBAL"}"

        when (val policy = worker.policy) {
            is WorkerPolicy.OneTime -> {
                val request = OneTimeWorkRequestBuilder<AndroidPlatformWorkerBridge>()
                    .setInitialDelay(policy.initialDelay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                    .build()

                workManager.enqueueUniqueWork(
                    "ONETIME_$uniqueTag",
                    ExistingWorkPolicy.KEEP,
                    request,
                )
            }

            is WorkerPolicy.Periodic -> {
                val flexInterval = policy.flexInterval?.inWholeMinutes ?: 15
                val request = PeriodicWorkRequestBuilder<AndroidPlatformWorkerBridge>(
                    policy.interval.inWholeMinutes.coerceAtLeast(15), TimeUnit.MINUTES,
                    flexInterval.coerceAtLeast(5), TimeUnit.MINUTES,
                )
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "PERIODIC_$uniqueTag",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            }
        }
        Napier.i(tag = TAG) { "Enqueued native Android WorkManager job for worker $uniqueTag" }
    }

    private fun buildAndroidConstraints(constraints: WorkerConstraints): Constraints {
        val networkType = when (constraints.network) {
            NetworkConstraint.NONE -> NetworkType.NOT_REQUIRED
            NetworkConstraint.CONNECTED -> NetworkType.CONNECTED
            NetworkConstraint.UNMETERED -> NetworkType.UNMETERED
        }

        return Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresCharging(constraints.requiresCharging)
            .setRequiresDeviceIdle(constraints.requiresDeviceIdle)
            .build()
    }
}
