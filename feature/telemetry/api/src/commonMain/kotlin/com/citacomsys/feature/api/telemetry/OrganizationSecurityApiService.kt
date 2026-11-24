package com.citacomsys.feature.api.telemetry

import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry

interface OrganizationSecurityApiService {
    suspend fun getAuditFeed(
        limit: Int
    ): List<AuditTimelineEntry>
}
