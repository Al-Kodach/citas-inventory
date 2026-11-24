package com.citacomsys.feature.api.telemetry

import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry

interface WorkspaceSecurityApiService {
    suspend fun getAuditFeed(
        branchId: String?,
        limit: Int
    ): List<AuditTimelineEntry>
    suspend fun getThreatSummary(workspaceId: String): List<SecurityThreatSummary>
}
