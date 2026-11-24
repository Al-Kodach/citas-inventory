package com.citacomsys.kernel.client.worker

import com.citacomsys.core.spi.NetworkConstraint
import com.citacomsys.core.spi.PlatformWorker
import com.citacomsys.core.spi.WorkerConstraints
import com.citacomsys.core.spi.WorkerPolicy
import com.citacomsys.core.spi.WorkerResult
import com.citacomsys.core.spi.WorkerScope
import com.citacomsys.kernel.client.OrganizationConfigStateManager
import io.github.aakira.napier.Napier
import kotlin.time.Duration.Companion.hours

/**
 * Global worker responsible for periodic background syncing of company-level OrganizationConfig.
 */
internal class OrgConfigSyncWorker(
    private val orgConfigStateManager: OrganizationConfigStateManager,
) : PlatformWorker {
    override val workerId: String = "org_config_sync_worker"
    override val workerScope: WorkerScope = WorkerScope.GLOBAL

    override val policy: WorkerPolicy = WorkerPolicy.Periodic(
        interval = 1.hours,
    )

    override val constraints: WorkerConstraints = WorkerConstraints(
        network = NetworkConstraint.CONNECTED,
    )

    override suspend fun doWork(workspaceId: String?): WorkerResult {
        return try {
            Napier.i(tag = "ORG_CONFIG_WORKER") { "OrgConfigSyncWorker executing company-level config refresh..." }
            orgConfigStateManager.refreshOrgConfig().getOrThrow()
            WorkerResult.SUCCESS
        } catch (e: Exception) {
            Napier.e(tag = "ORG_CONFIG_WORKER", throwable = e) { "Failed OrgConfigSyncWorker refresh cycle" }
            WorkerResult.RETRY
        }
    }
}
