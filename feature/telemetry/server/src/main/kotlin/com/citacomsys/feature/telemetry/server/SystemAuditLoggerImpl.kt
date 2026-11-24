package com.citacomsys.feature.telemetry.server

import com.citacomsys.feature.api.telemetry.*
import com.citacomsys.feature.telemetry.server.exposed.*
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.transaction.dbQuery
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.insert

internal class SystemAuditLoggerImpl : TelemetryAuditor {

    /**
     * Universally maps shared log metadata columns on any table extending BaseSiemTable.
     */
    private fun InsertStatement<*>.mapMetadata(table: BaseSiemTable, metadata: LogMetadata) {
        this[table.requestId] = metadata.requestId
        this[table.workspaceId] = metadata.workspaceId?.toUuid()
        this[table.organizationId] = metadata.orgId?.toUuid()
        this[table.branchId] = metadata.branchId?.toUuid()
        this[table.deviceId] = metadata.deviceId
        this[table.productType] = metadata.appFlavor
        this[table.level] = metadata.level.name
        this[table.createdAt] = metadata.createdAt
    }

    override suspend fun logIdentity(event: IdentityLog): Result<Boolean> = runCatching {
        dbQuery {
            IdentityAccessLogs.insert {
                it.mapMetadata(IdentityAccessLogs, event.metadata)

                it[userId] = event.userId
                it[action] = event.action
                it[outcome] = event.outcome
                it[failureReason] = event.failureReason
            }
            true
        }
    }

    override suspend fun logEndpoint(event: EndpointHostLog): Result<Boolean> = runCatching {
        dbQuery {
            EndpointHostLogs.insert {
                it.mapMetadata(EndpointHostLogs, event.metadata)
                it[osName] = event.osName
                it[architecture] = event.architecture
                it[action] = event.action.name
                it[fingerprintHash] = event.fingerprintHash
            }
            true
        }
    }

    override suspend fun logApplication(event: ApplicationLog): Result<Boolean> = runCatching {
        dbQuery {
            ApplicationAuditLogs.insert {
                it.mapMetadata(ApplicationAuditLogs, event.metadata)
                it[actorId] = event.actorId
                it[entityType] = event.entityType
                it[entityId] = event.entityId
                it[action] = event.action
                it[summary] = event.summary
                it[payloadChanges] = event.payloadChanges
            }
            true
        }
    }

    override suspend fun logNetworkSecurity(event: NetworkLog): Result<Boolean> =
        runCatching {
            dbQuery {
                NetworkSecurityLogs.insert {
                    it.mapMetadata(NetworkSecurityLogs, event.metadata)
                    it[clientIp] = event.clientIp
                    it[endpointRoute] = event.endpointRoute
                    it[triggerEvent] = event.triggerEvent
                    it[blockedDurationSeconds] = event.blockedDurationSeconds
                }
                true
            }
        }

    override suspend fun logOperational(event: OperationalLog): Result<Boolean> =
        runCatching {
            dbQuery {
                OperationalHealthLogs.insert {
                    it.mapMetadata(OperationalHealthLogs, event.metadata)
                    it[component] = event.component
                    it[message] = event.message
                    it[executionTimeMs] = event.executionTimeMs
                    it[stackTrace] = event.stack_trace
                }
                true
            }
        }
}
