package com.citacomsys.feature.api.telemetry

interface SystemEventService<T> {
    suspend fun findAfter(
        orgId: String,
        lastSeq: Long,
        workspaceId: String?
    ): List<T>
}
