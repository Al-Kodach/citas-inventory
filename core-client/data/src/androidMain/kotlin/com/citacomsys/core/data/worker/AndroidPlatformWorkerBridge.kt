package com.citacomsys.core.data.worker

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.citacomsys.core.spi.ForegroundNotificationInfo
import com.citacomsys.core.spi.PlatformWorker
import com.citacomsys.core.spi.WorkerResult
import io.github.aakira.napier.Napier
import org.koin.java.KoinJavaComponent.getKoin

/**
 * Android WorkManager bridge that retrieves the target [PlatformWorker] plugin from Koin by [workerId]
 * and executes its [PlatformWorker.doWork] method natively, supporting Foreground Service notifications.
 */
class AndroidPlatformWorkerBridge(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val workerId = inputData.getString(KEY_WORKER_ID)
        val targetWorker = workerId?.let { id ->
            runCatching { getKoin().getAll<PlatformWorker>().find { it.workerId == id } }.getOrNull()
        }
        val info = targetWorker?.foregroundInfo
        return if (info != null) {
            createForegroundInfo(applicationContext, info)
        } else {
            super.getForegroundInfo()
        }
    }

    override suspend fun doWork(): Result {
        val workerId = inputData.getString(KEY_WORKER_ID) ?: return Result.failure()
        val workspaceId = inputData.getString("KEY_WORKSPACE_ID").takeIf { !it.isNullOrBlank() }

        return try {
            val allWorkers = getKoin().getAll<PlatformWorker>()
            val targetWorker = allWorkers.find { it.workerId == workerId }

            if (targetWorker == null) {
                Napier.w(tag = "WORKER_BRIDGE") { "No PlatformWorker registered with id: $workerId" }
                return Result.failure()
            }

            // Promote to Foreground Service if foregroundInfo is specified and allowed by OS
            targetWorker.foregroundInfo?.let { info ->
                runCatching {
                    val foregroundInfo = createForegroundInfo(applicationContext, info)
                    setForeground(foregroundInfo)
                    Napier.i(tag = "WORKER_BRIDGE") { "Promoted $workerId to Foreground Service with notification: ${info.title}" }
                }.onFailure { e ->
                    Napier.w(tag = "WORKER_BRIDGE", throwable = e) { "Failed to promote $workerId to foreground service: ${e.message}" }
                }
            }

            Napier.i(tag = "WORKER_BRIDGE") { "Executing PlatformWorker bridge task: $workerId (Workspace: ${workspaceId ?: "GLOBAL"})..." }
            val result = targetWorker.doWork(workspaceId)

            when (result) {
                WorkerResult.SUCCESS -> Result.success()
                WorkerResult.RETRY -> if (runAttemptCount < 3) Result.retry() else Result.failure()
                WorkerResult.FAILURE -> Result.failure()
            }
        } catch (e: Exception) {
            Napier.e(tag = "WORKER_BRIDGE", throwable = e) { "Failed to execute PlatformWorker bridge task: $workerId" }
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(
        context: Context,
        info: ForegroundNotificationInfo
    ): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                info.channelId,
                info.channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, info.channelId)
            .setContentTitle(info.title)
            .setContentText(info.message)
            .setSmallIcon(R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        return ForegroundInfo(info.notificationId, notification)
    }

    companion object {
        const val KEY_WORKER_ID = "KEY_PLATFORM_WORKER_ID"
    }
}
