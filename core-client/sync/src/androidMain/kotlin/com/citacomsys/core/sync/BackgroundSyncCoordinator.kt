package com.citacomsys.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BackgroundSyncCoordinator(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun triggerReliableBackgroundSync() {
        // Enforce network constraints before letting the bridge wake the engine
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<BackgroundSyncBridgeWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "PLATFORM_BACKGROUND_CATCHUP",
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }
}
