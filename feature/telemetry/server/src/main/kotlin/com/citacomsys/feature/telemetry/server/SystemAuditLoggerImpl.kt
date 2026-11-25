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
                it[IdentityAccessLogs.organizationId] = event.metadata.orgId?.toUuid()
                it[IdentityAccessLogs.workspaceId] = event.metadata.workspaceId?.toUuid()
                it[IdentityAccessLogs.userId] = event.userId
                it[IdentityAccessLogs.action] = event.action
                it[IdentityAccessLogs.outcome] = event.outcome
                it[IdentityAccessLogs.failureReason] = event.failureReason
                it[IdentityAccessLogs.createdAt] = event.metadata.createdAt
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
                    it[NetworkSecurityLogs.clientIp] = event.clientIp
                    it[NetworkSecurityLogs.endpointRoute] = event.endpointRoute
                    it[NetworkSecurityLogs.triggerEvent] = event.triggerEvent
                    it[NetworkSecurityLogs.blockedDurationSeconds] = event.blockedDurationSeconds
                    it[NetworkSecurityLogs.createdAt] = event.metadata.createdAt
                }
                true
            }
        }

    override suspend fun logOperational(event: OperationalLog): Result<Boolean> =
        runCatching {
            dbQuery {
                OperationalHealthLogs.insert {
                    it[OperationalHealthLogs.component] = event.component
                    it[OperationalHealthLogs.message] = event.message
                    it[OperationalHealthLogs.executionTimeMs] = event.executionTimeMs
                    it[OperationalHealthLogs.createdAt] = event.metadata.createdAt
                }
                true
            }
        }
}
