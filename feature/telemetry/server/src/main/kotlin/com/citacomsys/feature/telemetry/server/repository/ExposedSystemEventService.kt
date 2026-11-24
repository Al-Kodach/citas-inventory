package com.citacomsys.feature.telemetry.server.repository

import com.citacomsys.feature.api.telemetry.SystemEventService
import com.citacomsys.feature.telemetry.server.exposed.SystemEvents
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.shared.event.EntityChangeEvent
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll

internal class ExposedSystemEventService : SystemEventService<EntityChangeEvent> {
    override suspend fun findAfter(
        orgId: String,
        lastSeq: Long,
        workspaceId: String?
    ): List<EntityChangeEvent> = dbQuery {
        SystemEvents.innerJoin(Workspaces)
            .selectAll()
            .where {
                SystemEvents.seq greater lastSeq and
                        (SystemEvents.organizationId eq orgId.toUuid()) or
                        (SystemEvents.workspaceId eq workspaceId?.toUuid())
            }.map {
                EntityChangeEvent(
                    seq = it[SystemEvents.seq],
                    organizationId = it[Workspaces.organizationId].toString(),
                    entityType = it[SystemEvents.entityType],
                    entityId = it[SystemEvents.entityId],
                    timestamp = it[SystemEvents.createdAt],
                    action = it[SystemEvents.action],
                    workspaceId = it[SystemEvents.workspaceId]?.toString(),
                    branchId = it[SystemEvents.branchId]?.toString()
                )
            }
    }
}
