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
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class SiemTraceRepository {

    suspend fun getTopSecurityThreats(limit: Int = 10): List<SecurityThreatSummary> = dbQuery {
        val totalViolationsCount = NetworkSecurityLogs.id.count()
        val maxCreatedAt = NetworkSecurityLogs.createdAt.max()

        NetworkSecurityLogs
            .select(
                NetworkSecurityLogs.clientIp,
                totalViolationsCount,
                maxCreatedAt
            )
            .groupBy(
                NetworkSecurityLogs.clientIp
            )
            .orderBy(totalViolationsCount to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                SecurityThreatSummary(
                    clientIp = row[NetworkSecurityLogs.clientIp],
                    deviceId = null,
                    appFlavor = "SYSTEM",
                    totalViolations = row[totalViolationsCount],
                    lastTriggeredEvent = "NETWORK_THREAT",
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


    @Suppress("UNUSED_PARAMETER")
    suspend fun getTimelineByDeviceId(deviceId: String): List<TraceTimelineEntry> = dbQuery {
        val timeline = mutableListOf<TraceTimelineEntry>()
        timeline.sortByDescending { it.createdAt }
        timeline
    }

    suspend fun getTimelineByRequestId(requestId: String): List<TraceTimelineEntry> = dbQuery {
        val timeline = mutableListOf<TraceTimelineEntry>()

        ApplicationAuditLogs.selectAll().where { ApplicationAuditLogs.requestId eq requestId }
            .forEach { timeline.add(it.toTimelineEntry("APPLICATION")) }

        timeline.sortBy { it.createdAt }
        timeline
    }

    private fun ResultRow.toTimelineEntry(source: String): TraceTimelineEntry {
        return when (source) {
            "IDENTITY" -> TraceTimelineEntry(
                requestId = "",
                logSource = "IDENTITY",
                level = "INFO",
                createdAt = this[IdentityAccessLogs.createdAt],
                actionOrComponent = this[IdentityAccessLogs.action],
                description = "Outcome: ${this[IdentityAccessLogs.outcome]}",
                organizationId = this[IdentityAccessLogs.organizationId]?.toString(),
                workspaceId = this[IdentityAccessLogs.workspaceId]?.toString(),
                deviceId = null
            )
            "NETWORK" -> TraceTimelineEntry(
                requestId = "",
                logSource = "NETWORK",
                level = "INFO",
                createdAt = this[NetworkSecurityLogs.createdAt],
                actionOrComponent = this[NetworkSecurityLogs.triggerEvent],
                description = "IP: ${this[NetworkSecurityLogs.clientIp]} -> ${this[NetworkSecurityLogs.endpointRoute]}",
                organizationId = null,
                workspaceId = null,
                deviceId = null
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
                requestId = "",
                logSource = "OPERATIONAL",
                level = "INFO",
                createdAt = this[OperationalHealthLogs.createdAt],
                actionOrComponent = this[OperationalHealthLogs.component],
                description = this[OperationalHealthLogs.message],
                organizationId = null,
                workspaceId = null,
                deviceId = null
            )
            else -> throw IllegalArgumentException("Unknown source")
        }
    }
}
