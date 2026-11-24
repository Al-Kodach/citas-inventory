package com.citacomsys.kernel.client.worker

import com.citacomsys.core.spi.NetworkConstraint
import com.citacomsys.core.spi.PlatformWorker
import com.citacomsys.core.spi.WorkerConstraints
import com.citacomsys.core.spi.WorkerPolicy
import com.citacomsys.core.spi.WorkerResult
import com.citacomsys.core.spi.WorkerScope
import com.citacomsys.kernel.client.WorkspaceEnvironmentManager
import io.github.aakira.napier.Napier
import kotlin.time.Duration.Companion.hours

/**
 * Workspace-scoped worker responsible for syncing container profile & state for active workspaces.
 */
internal class WorkspaceEnvironmentSyncWorker(
    private val workspaceEnvironmentManager: WorkspaceEnvironmentManager,
) : PlatformWorker {
    override val workerId: String = "workspace_environment_sync_worker"
    override val workerScope: WorkerScope = WorkerScope.WORKSPACE_SCOPED

    override val policy: WorkerPolicy = WorkerPolicy.Periodic(
        interval = 1.hours,
    )

    override val constraints: WorkerConstraints = WorkerConstraints(
        network = NetworkConstraint.CONNECTED,
    )

    override suspend fun doWork(workspaceId: String?): WorkerResult {
        if (workspaceId.isNullOrBlank()) {
            Napier.d(tag = "WORKSPACE_ENV_WORKER") { "No active workspace ID provided. Skipping workspace environment sync." }
            return WorkerResult.SUCCESS
        }

        return try {
            Napier.i(tag = "WORKSPACE_ENV_WORKER") { "WorkspaceEnvironmentSyncWorker refreshing container state for workspace $workspaceId..." }
            workspaceEnvironmentManager.refreshWorkspace(workspaceId).getOrThrow()
            WorkerResult.SUCCESS
        } catch (e: Exception) {
            Napier.e(tag = "WORKSPACE_ENV_WORKER", throwable = e) { "Failed WorkspaceEnvironmentSyncWorker refresh cycle for workspace $workspaceId" }
            WorkerResult.RETRY
        }
    }
}
