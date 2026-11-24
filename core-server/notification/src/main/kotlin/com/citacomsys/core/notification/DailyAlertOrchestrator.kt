package com.citacomsys.core.notification

import com.citacomsys.core.persistence.table.tenant.WorkspaceModules
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.transaction.dbQuery
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory

/**
 * Central orchestrator for all daily background tasks.
 * It identifies active workspaces and delegates work to registered [AlertProcessor]s.
 */
class DailyAlertOrchestrator(
    private val processors: List<AlertProcessor>
) {
    private val logger = LoggerFactory.getLogger(DailyAlertOrchestrator::class.java)

    /**
     * Master processing sequence. Triggered daily via a background worker.
     */
    suspend fun processDailyAlertPipeline() {
        logger.info("Initiating system-wide automated alerts processing cycle...")

        val workspaceModulePairs = dbQuery {
            WorkspaceModules
                .innerJoin(Workspaces)
                .selectAll()
                .map { it[Workspaces.id].toString() to it[WorkspaceModules.module] }
        }

        // Group by orgId to avoid multiple scans
        val workspaceWorkMap = workspaceModulePairs.groupBy({ it.first }, { it.second })

        workspaceWorkMap.forEach { (workspaceId, modules) ->
            val activeModules = modules.toSet()

            processors.forEach { processor ->
                if (processor.shouldProcess(activeModules)) {
                    try {
                        logger.debug("Executing processor '${processor.processorId}' for workspace $workspaceId")
                        processor.process(workspaceId)
                    } catch (e: Exception) {
                        logger.error("Processor '${processor.processorId}' failed for workspace $workspaceId", e)
                    }
                }
            }
        }

        logger.info("Daily automated alert pipeline completed.")
    }
}