package com.citacomsys.feature.telemetry.server.repository

import com.citacomsys.feature.api.telemetry.ComponentFailureSummary
import com.citacomsys.feature.api.telemetry.SecurityThreatSummary
import com.citacomsys.feature.api.telemetry.TraceTimelineEntry
import com.citacomsys.feature.telemetry.server.exposed.ApplicationAuditLogs
import com.citacomsys.feature.telemetry.server.exposed.IdentityAccessLogs
import com.citacomsys.feature.telemetry.server.exposed.NetworkSecurityLogs
import com.citacomsys.feature.telemetry.server.exposed.OperationalHealthLogs
import com.citacomsys.core.persistence.transaction.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

class SiemTraceRepository {

    suspend fun getTopSecurityThreats(limit: Int = 10): List<SecurityThreatSummary> = dbQuery {
        val totalViolationsCount = NetworkSecurityLogs.id.count()
        val maxCreatedAt = NetworkSecurityLogs.createdAt.max()

        NetworkSecurityLogs
            .select(
                NetworkSecurityLogs.clientIp,
                NetworkSecurityLogs.deviceId,
                NetworkSecurityLogs.productType,
                totalViolationsCount,
                maxCreatedAt
            )
            .groupBy(
                NetworkSecurityLogs.clientIp,
                NetworkSecurityLogs.deviceId,
                NetworkSecurityLogs.productType
            )
            .orderBy(totalViolationsCount to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                SecurityThreatSummary(
                    clientIp = row[NetworkSecurityLogs.clientIp],
                    deviceId = row[NetworkSecurityLogs.deviceId],
                    appFlavor = row[NetworkSecurityLogs.productType],
                    totalViolations = row[totalViolationsCount],
                    lastTriggeredEvent = "AGGREGATED_THREAT_SIGNATURE",
                    lastOccurredAt = row[maxCreatedAt]!!
                )
            }
    }

    suspend fun getCommonOperationalFailures(limit: Int = 10): List<ComponentFailureSummary> =
        dbQuery {
            val failureCount = OperationalHealthLogs.id.count()
            val maxCreatedAt = OperationalHealthLogs.createdAt.max()

            OperationalHealthLogs
                .select(
                    OperationalHealthLogs.component,
                    failureCount,
                    maxCreatedAt
                )
                .where { OperationalHealthLogs.level inList listOf("ERROR", "CRITICAL") }
                .groupBy(OperationalHealthLogs.component)
                .orderBy(failureCount to SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    val componentName = row[OperationalHealthLogs.component]

                    val sampleMsg = OperationalHealthLogs
                        .select(OperationalHealthLogs.message)
                        .where { OperationalHealthLogs.component eq componentName }
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


    suspend fun getGlobalSecurityFeed(limit: Int = 50): List<TraceTimelineEntry> = dbQuery {
        val timeline = mutableListOf<TraceTimelineEntry>()

        IdentityAccessLogs.selectAll().orderBy(IdentityAccessLogs.createdAt, SortOrder.DESC).limit(limit).forEach {
            timeline.add(it.toTimelineEntry("IDENTITY"))
        }
        NetworkSecurityLogs.selectAll().orderBy(NetworkSecurityLogs.createdAt, SortOrder.DESC).limit(limit).forEach {
            timeline.add(it.toTimelineEntry("NETWORK"))
        }
        ApplicationAuditLogs.selectAll().orderBy(ApplicationAuditLogs.createdAt, SortOrder.DESC).limit(limit).forEach {
            timeline.add(it.toTimelineEntry("APPLICATION"))
        }

        timeline.sortByDescending { it.createdAt }
        timeline.take(limit)
    }


    suspend fun getTimelineByDeviceId(deviceId: String): List<TraceTimelineEntry> = dbQuery {
        val timeline = mutableListOf<TraceTimelineEntry>()

        IdentityAccessLogs.selectAll().where { IdentityAccessLogs.deviceId eq deviceId }
            .forEach { timeline.add(it.toTimelineEntry("IDENTITY")) }

        NetworkSecurityLogs.selectAll().where { NetworkSecurityLogs.deviceId eq deviceId }
            .forEach { timeline.add(it.toTimelineEntry("NETWORK")) }

        timeline.sortByDescending { it.createdAt }
        timeline
    }

    suspend fun getTimelineByRequestId(requestId: String): List<TraceTimelineEntry> = dbQuery {
        val timeline = mutableListOf<TraceTimelineEntry>()

        IdentityAccessLogs.selectAll().where { IdentityAccessLogs.requestId eq requestId }
            .forEach { timeline.add(it.toTimelineEntry("IDENTITY")) }

        ApplicationAuditLogs.selectAll().where { ApplicationAuditLogs.requestId eq requestId }
            .forEach { timeline.add(it.toTimelineEntry("APPLICATION")) }

        NetworkSecurityLogs.selectAll().where { NetworkSecurityLogs.requestId eq requestId }
            .forEach { timeline.add(it.toTimelineEntry("NETWORK")) }

        OperationalHealthLogs.selectAll().where { OperationalHealthLogs.requestId eq requestId }
            .forEach { timeline.add(it.toTimelineEntry("OPERATIONAL")) }

        timeline.sortBy { it.createdAt }
        timeline
    }

    private fun ResultRow.toTimelineEntry(source: String): TraceTimelineEntry {
        return when (source) {
            "IDENTITY" -> TraceTimelineEntry(
                requestId = this[IdentityAccessLogs.requestId],
                logSource = "IDENTITY",
                level = this[IdentityAccessLogs.level],
                createdAt = this[IdentityAccessLogs.createdAt],
                actionOrComponent = this[IdentityAccessLogs.action],
                description = "Outcome: ${this[IdentityAccessLogs.outcome]}",
                organizationId = this[IdentityAccessLogs.organizationId]?.toString(),
                workspaceId = this[IdentityAccessLogs.workspaceId]?.toString(),
                deviceId = this[IdentityAccessLogs.deviceId]
            )
            "NETWORK" -> TraceTimelineEntry(
                requestId = this[NetworkSecurityLogs.requestId],
                logSource = "NETWORK",
                level = this[NetworkSecurityLogs.level],
                createdAt = this[NetworkSecurityLogs.createdAt],
                actionOrComponent = this[NetworkSecurityLogs.triggerEvent],
                description = this[NetworkSecurityLogs.endpointRoute],
                organizationId = this[NetworkSecurityLogs.organizationId]?.toString(),
                workspaceId = this[NetworkSecurityLogs.workspaceId]?.toString(),
                deviceId = this[NetworkSecurityLogs.deviceId]
            )
            "APPLICATION" -> TraceTimelineEntry(
                requestId = this[ApplicationAuditLogs.requestId],
                logSource = "APPLICATION",
                level = this[ApplicationAuditLogs.level],
                createdAt = this[ApplicationAuditLogs.createdAt],
                actionOrComponent = this[ApplicationAuditLogs.action],
                description = this[ApplicationAuditLogs.summary],
                organizationId = this[ApplicationAuditLogs.organizationId]?.toString(),
                workspaceId = this[ApplicationAuditLogs.workspaceId]?.toString(),
                deviceId = this[ApplicationAuditLogs.deviceId]
            )
            "OPERATIONAL" -> TraceTimelineEntry(
                requestId = this[OperationalHealthLogs.requestId],
                logSource = "OPERATIONAL",
                level = this[OperationalHealthLogs.level],
                createdAt = this[OperationalHealthLogs.createdAt],
                actionOrComponent = this[OperationalHealthLogs.component],
                description = this[OperationalHealthLogs.message],
                organizationId = this[OperationalHealthLogs.organizationId]?.toString(),
                workspaceId = this[OperationalHealthLogs.workspaceId]?.toString(),
                deviceId = this[OperationalHealthLogs.deviceId]
            )
            else -> throw IllegalArgumentException("Unknown source")
        }
    }
}
