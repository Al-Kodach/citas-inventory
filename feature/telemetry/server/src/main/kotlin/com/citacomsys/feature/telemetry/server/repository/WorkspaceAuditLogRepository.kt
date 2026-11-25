package com.citacomsys.feature.telemetry.server.repository

import com.citacomsys.feature.api.telemetry.ComponentFailureSummary
import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry
import com.citacomsys.feature.telemetry.server.exposed.ApplicationAuditLogs
import com.citacomsys.feature.telemetry.server.exposed.IdentityAccessLogs
import com.citacomsys.feature.telemetry.server.exposed.OperationalHealthLogs
import com.citacomsys.core.persistence.transaction.dbQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID


class WorkspaceAuditLogRepository {

    /**
     * Captures a live audit feed of all operational changes inside a workspace.
     * Optionally filters down to a single branch context.
     */
    suspend fun getWorkspaceAuditFeed(
        workspaceId: String,
        branchId: String?,
        limit: Int = 50
    ): List<AuditTimelineEntry> = dbQuery {
        val tenantUuid = UUID.fromString(workspaceId)
        val branchUuid = branchId?.let { UUID.fromString(it) }
        val timeline = mutableListOf<AuditTimelineEntry>()

        IdentityAccessLogs
            .selectAll()
            .where { IdentityAccessLogs.workspaceId eq tenantUuid }
            .orderBy(IdentityAccessLogs.createdAt, SortOrder.DESC)
            .limit(limit)
            .forEach { timeline.add(it.toTenantTimeline("IDENTITY")) }

        ApplicationAuditLogs
            .selectAll()
            .where {
                val expr = ApplicationAuditLogs.workspaceId eq tenantUuid
                if (branchUuid != null) expr and (ApplicationAuditLogs.branchId eq branchUuid) else expr
            }
            .orderBy(ApplicationAuditLogs.createdAt, SortOrder.DESC)
            .limit(limit)
            .forEach { timeline.add(it.toTenantTimeline("APPLICATION")) }

        timeline.sortByDescending { it.createdAt }
        return@dbQuery timeline.take(limit)
    }

    /**
     * Captures a live audit feed of all operational changes inside an organization (across all workspaces).
     */
    suspend fun getOrganizationAuditFeed(
        organizationId: String,
        limit: Int = 100
    ): List<AuditTimelineEntry> = dbQuery {
        val orgUuid = UUID.fromString(organizationId)
        val timeline = mutableListOf<AuditTimelineEntry>()

        IdentityAccessLogs
            .selectAll()
            .where { IdentityAccessLogs.organizationId eq orgUuid }
            .orderBy(IdentityAccessLogs.createdAt, SortOrder.DESC)
            .limit(limit)
            .forEach { timeline.add(it.toTenantTimeline("IDENTITY")) }

        ApplicationAuditLogs
            .selectAll()
            .where { ApplicationAuditLogs.organizationId eq orgUuid }
            .orderBy(ApplicationAuditLogs.createdAt, SortOrder.DESC)
            .limit(limit)
            .forEach { timeline.add(it.toTenantTimeline("APPLICATION")) }

        timeline.sortByDescending { it.createdAt }
        return@dbQuery timeline.take(limit)
    }

    /**
     * Spots localized operational bottlenecks or errors inside a single branch registry.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun getBranchOperationalFailures(
        tenantId: String,
        branchId: String,
        limit: Int = 10
    ): List<ComponentFailureSummary> = dbQuery {
        val failureCount = OperationalHealthLogs.id.count()
        val maxCreatedAt = OperationalHealthLogs.createdAt.max()

        OperationalHealthLogs
            .select(OperationalHealthLogs.component, failureCount, maxCreatedAt)
            .groupBy(OperationalHealthLogs.component)
            .orderBy(failureCount to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                val componentName = row[OperationalHealthLogs.component]

                val sampleMsg = OperationalHealthLogs
                    .select(OperationalHealthLogs.message)
                    .where {
                        OperationalHealthLogs.component eq componentName
                    }
                    .orderBy(OperationalHealthLogs.createdAt to SortOrder.DESC)
                    .limit(1)
                    .map { it[OperationalHealthLogs.message] }
                    .firstOrNull() ?: "No message trace available"

                ComponentFailureSummary(
                    componentName = componentName,
                    totalFailures = row[failureCount],
                    exampleErrorMessage = sampleMsg.take(120),
                    lastOccurredAt = row[maxCreatedAt]!!
                )
            }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toTenantTimeline(source: String): AuditTimelineEntry {
        return when (source) {
            "IDENTITY" -> AuditTimelineEntry(
                requestId = "",
                level = "INFO",
                logSource = "IDENTITY",
                createdAt = this[IdentityAccessLogs.createdAt],
                action = this[IdentityAccessLogs.action],
                summary = "Outcome: ${this[IdentityAccessLogs.outcome]}. Context: ${this[IdentityAccessLogs.failureReason]}",
                userId = this[IdentityAccessLogs.userId],
                branchId = null,
                workspaceId = this[IdentityAccessLogs.workspaceId]?.toString()
            )

            "APPLICATION" -> AuditTimelineEntry(
                requestId = this[ApplicationAuditLogs.requestId],
                logSource = "APPLICATION",
                level = this[ApplicationAuditLogs.level],
                createdAt = this[ApplicationAuditLogs.createdAt],
                action = this[ApplicationAuditLogs.action],
                summary = this[ApplicationAuditLogs.summary],
                userId = this[ApplicationAuditLogs.actorId],
                branchId = this[ApplicationAuditLogs.branchId]?.toString(),
                workspaceId = this[ApplicationAuditLogs.workspaceId]?.toString()
            )

            else -> throw IllegalArgumentException("Unknown source context: $source")
        }
    }
}
